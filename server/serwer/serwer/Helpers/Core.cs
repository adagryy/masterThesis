using serwer.Config;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Threading;
using System.Web;
using System.Web.Mvc;

namespace serwer.Helpers
{
    static class Core
    {
        public static bool checkIfMatlabScriptExistsOnServer(string matlabScriptsDirectory, string selectedAlgorithm)
        {
            return File.Exists(matlabScriptsDirectory + selectedAlgorithm + ".m");
        }        

        // Starts processing of image uploaded to the server. 
        // "file" - file of image to be processed
        // "user" - tells which signed in user uploaded an image and requested processing
        // "selectedAlgorithm" - name of matlab algorithm available on the server to be used for image processing
        public static void startProcessingImage(HttpPostedFileBase file, String user, String selectedAlgorithm)
        {
            removePersonalImageFiles(HttpContext.Current.Server.MapPath(ServerConfigurator.imageStoragePath + user + "/")); // Remove previous files       

            string fileName = Path.GetFileName(file.FileName);
            string storageImageSourceDirectory = HttpContext.Current.Server.MapPath(ServerConfigurator.imageStoragePath + user + "/"); // directory into which we will save the original image
            string storageImageDestinationDirectory = HttpContext.Current.Server.MapPath(ServerConfigurator.imageStoragePath + user + "/"); // directory into which we will save the original image
            string storageAfterPOrocessingJSONFileNameDirectory = HttpContext.Current.Server.MapPath(ServerConfigurator.imageStoragePath + user + "/"); // directory into which we will save the original image
            string originalFileName = ServerConfigurator.originalImageName + Path.GetExtension(file.FileName); // name of file which is used to save the uploaded image on the server
            string processedFileName = ServerConfigurator.processedImageName + Path.GetExtension(file.FileName); // name of file which will be used to save image after processing
            string matlabScriptsDirectory = HttpContext.Current.Server.MapPath(ServerConfigurator.matlabScriptsPath); // in this directory are stored all matlab scripts

            string path = Path.Combine(storageImageSourceDirectory, originalFileName); // path for save uploaded image to server disk
            file.SaveAs(path);

            // Create object which will be passed to new Thread for image processing
            MatlabProcessingDataThreaded matlabProcessingDataThreaded = new MatlabProcessingDataThreaded(
                    storageImageSourceDirectory, // Directory in which there are stored images for reading to processing. This path is user personalised, eg. "~/Storage/testuser/"
                    storageImageDestinationDirectory, // Directory in which there is saved an image after processing by Matlab. It may be the same directory as the images storing directory
                    storageAfterPOrocessingJSONFileNameDirectory, // Directory in which there is saved an JSON file created by Matlab during image processing. It may be the same directory as the images storing directory
                    ServerConfigurator.afterProcessingDataFileName, // Filename of the above file
                    ServerConfigurator.afterProcessingDataFileExtension, // File extension of the above file
                    originalFileName,  // Filename of image, which will be processed
                    processedFileName,  // Filename after processing
                    selectedAlgorithm, // Tells to Matlab, which algorithm to use for image processing
                    matlabScriptsDirectory // Tells the matlab where algorithms are stored on the server.
                );

            // Threads are used to perform time-consuming image processing using matlab
            //ThreadStart threadStart = new ThreadStart(processImageWithinNewThread);
            Thread thread = new Thread(() => processImageWithinNewThread(matlabProcessingDataThreaded));
            thread.Start();
        }

        // This method is run in new Thread which ensures, that application is not blocked when running time-consuming image processing
        // It takes "MatlabProcessingDataThreaded" object, which has all required parameters by this method and ensures, that there is no 
        // any shared variables problems
        private static void processImageWithinNewThread(MatlabProcessingDataThreaded matlabProcessingDataThreaded)
        {
            try
            {
                // For more information see official MathWorks doccumentation on how to use MLApp Matlab object reference
                MLApp.MLApp matlab = new MLApp.MLApp();
                matlab.Execute("cd " + matlabProcessingDataThreaded.MatlabScriptsDirectory); // move Matlab shell context to the directory specified here, e. g. "cd F:\\Resources\\processImage.m"
                object result = null; // output data
                matlab.Feval(
                        matlabProcessingDataThreaded.SelectedProcessingAlgorithm, // The name of algoritm which will be used for processing
                        0, // Number of output arguments
                        out result, // Output data
                        matlabProcessingDataThreaded.ImageSource + matlabProcessingDataThreaded.OriginalFileName, // Parameter #1 to matlab algorithm (source image path with filename)
                        matlabProcessingDataThreaded.ImageDestination + matlabProcessingDataThreaded.ProcessedFileName, // Parameter #2 to matlab algorithm (destination image path with filename)
                        matlabProcessingDataThreaded.AfterProcessingFileDestination + matlabProcessingDataThreaded.AfterProcessingFileName + matlabProcessingDataThreaded.AfterProcessingFileExtension // Parameter #3 to matlab algorithm (full destination path where to save JSON data)
                    );
                matlab.Quit();
            }
            catch (System.Runtime.InteropServices.COMException e)
            {
                //ViewBag.MatlabProcessingError = "Error processing image on the server. Probably incorrect uage of matlab function available on the server";
            }
            catch (Exception e)
            {
                Debug.WriteLine(e);
                //ViewBag.MatlabProcessingError = "Unknown image processing error";
            }

        }

        private static void removePersonalImageFiles(string directoryPath)
        {
            DirectoryInfo di = new DirectoryInfo(directoryPath); // List to clear images in current personal images' directory

            foreach (FileInfo fileForDeletion in di.GetFiles())
            {
                fileForDeletion.Delete();
            }
        }
    }
}