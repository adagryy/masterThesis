using System;
using System.Web;
using System.Web.Mvc;
using System.IO;
using serwer.Models;
using serwer.Config;
using System.Collections.Generic;
using System.Threading;
using serwer.Helpers;
using Newtonsoft.Json;

namespace serwer.Controllers
{
    [Authorize]
    public class ImagesController : Controller
    {
        // GET: Images
        public ActionResult Index()
        {
            return View();
        }

        // This action represents a view for image uploading
        [HttpGet]
        public ActionResult UploadFile()
        {
            UploadFileViewModel uploadFileViewModel = new UploadFileViewModel(); // ViewModel for passing a list of available matlab algorithms on the server to the client

            string[] files = Directory.GetFiles(Server.MapPath(ServerConfigurator.matlabScriptsPath)); // Read all matlab algorithms available on server

            List<SelectListItem> list = new List<SelectListItem>(); // create list of algorithms later passed to the view
            list.Add(new SelectListItem { Selected = true, Text = "Wybierz algorytm", Value = "-1" }); // default (first) value "Wybierz algorytm"

            foreach (var file in files) // add all algorithm to the list
            {
                string fileName = Path.GetFileNameWithoutExtension(file);
                list.Add(new SelectListItem { Selected = false, Text = fileName, Value = fileName });
            }

            uploadFileViewModel.algorithms = new SelectList(list); // save list to the viewmodel "SelectList" object

            return View(uploadFileViewModel);
        }

        // This action handles a HTTP POST request with image uploaded
        [HttpPost]
        [ValidateAntiForgeryToken]
        public ActionResult UploadFile(HttpPostedFileBase file, UploadFileViewModel model)
        {

            try
            {
                if (file.ContentLength > 0)
                {
                    DirectoryInfo di = new DirectoryInfo(Server.MapPath(ServerConfigurator.imageStoragePath + User.Identity.Name +"/")); // List to clear images in current personal images' directory

                    foreach (FileInfo fileForDeletion in di.GetFiles())
                    {
                        fileForDeletion.Delete();
                    }

                    string user = User.Identity.Name; // fetches the user login, which is currently logged in. It is used to decide into which directory the image should be saved
                    
                    string fileName = Path.GetFileName(file.FileName); 
                    string storageImageSourceDirectory = Server.MapPath(ServerConfigurator.imageStoragePath + user + "/"); // directory into which we will save the original image
                    string storageImageDestinationDirectory = Server.MapPath(ServerConfigurator.imageStoragePath + user + "/"); // directory into which we will save the original image
                    string storageAfterPOrocessingJSONFileNameDirectory = Server.MapPath(ServerConfigurator.imageStoragePath + user + "/"); // directory into which we will save the original image

                    string originalFileName = ServerConfigurator.originalImageName + Path.GetExtension(file.FileName); // name of file which is used to save the uploaded image on the server
                    string processedFileName = ServerConfigurator.processedImageName + Path.GetExtension(file.FileName); // name of file which will be used to save image after processing

                    string matlabScriptsDirectory = Server.MapPath(ServerConfigurator.matlabScriptsPath); // in this directory are stored all matlab scripts

                    string path = Path.Combine(storageImageSourceDirectory, originalFileName); // path for save uploaded image to server disk
                    file.SaveAs(path);                    

                    if (!System.IO.File.Exists(matlabScriptsDirectory + model.selectedAlgorithm + ".m")) // Check if given algorithm exists on the server
                    {
                        ViewBag.Message = "Błąd - nie ma takiego algorytmu na serwerze";
                        return View("imagesView", this.getImages());
                    }

                    // Create object which will be passed to new Thread for image processing
                    MatlabProcessingDataThreaded matlabProcessingDataThreaded = new MatlabProcessingDataThreaded(
                            storageImageSourceDirectory, // Directory in which there are stored images for reading to processing. This path is user personalised, eg. "~/Storage/testuser/"
                            storageImageDestinationDirectory, // Directory in which there is saved an image after processing by Matlab. It may be the same directory as the images storing directory
                            storageAfterPOrocessingJSONFileNameDirectory, // Directory in which there is saved an JSON file created by Matlab during image processing. It may be the same directory as the images storing directory
                            ServerConfigurator.afterProcessingDataFileName, // Filename of the above file
                            ServerConfigurator.afterProcessingDataFileExtension, // File extension of the above file
                            originalFileName,  // Filename of image, which will be processed
                            processedFileName,  // Filename after processing
                            model.selectedAlgorithm, // Tells to Matlab, which algorithm to use for image processing
                            matlabScriptsDirectory // Tells the matlab where algorithms are stored on the server.
                        );

                    // Threads are used to perform time-consuming image processing using matlab
                    //ThreadStart threadStart = new ThreadStart(processImageWithinNewThread);
                    Thread thread = new Thread(() => processImageWithinNewThread(matlabProcessingDataThreaded));
                    thread.Start();

                }
                ViewBag.Message = "Pomyślnie przesłano obraz na serwer";
                return RedirectToAction("imagesView");
            }
            catch (Exception e)
            {
                ViewBag.Message = "Error during sending image to server";// "File upload failed!!";
                return View();
            }
        }

        // This method is run in new Thread which ensures, that application is not blocked when running time-consuming image processing
        // It takes "MatlabProcessingDataThreaded" object, which has all required parameters by this method and ensures, that there is no 
        // any shared variables problems
        private void processImageWithinNewThread(MatlabProcessingDataThreaded matlabProcessingDataThreaded)
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
                ViewBag.MatlabProcessingError = "Error processing image on the server. Probably incorrect uage of matlab function available on the server";
            }
            catch (Exception) { ViewBag.MatlabProcessingError = "Unknown image processing error"; }

        }

        [HttpGet]
        public ActionResult ImagesView()
        {
            return View(this.getImages());
        }

        // Returns an image for download
        [HttpGet]
        public FileResult Download(string ImageName)
        {
            return File(Server.MapPath(ServerConfigurator.imageStoragePath + User.Identity.Name + "/" + ImageName), System.Net.Mime.MediaTypeNames.Application.Octet, ImageName);
        }

        [HttpPost]
        [ValidateAntiForgeryToken]
        public ActionResult RemoveImage(ImagesDownloadDetails imagesDownloadDetails)
        {
            try
            {
                System.IO.File.Delete(Server.MapPath(imagesDownloadDetails.removeImagePath));
            }catch(ArgumentException) { }
            //catch (ArgumentNullException) { } // 
            catch(DirectoryNotFoundException) { }
            catch (IOException) { }
            catch (NotSupportedException) { }
            //catch (PathTooLongException) { } // already catched by IOException
            catch (UnauthorizedAccessException) { }

            return RedirectToAction("ImagesView");
        }

        // Saves all images names in directory to viewmodel object to retrieve them during displaying images in the view named "ImagesView"
        // There SHOULD ALWAYS be only two images per user on the server, but this method supports more, than two
        private ImagesDownloadDetails getImages()
        {
            ImagesDownloadDetails imagesDownloadDetails = new ImagesDownloadDetails(); // New ViewModel object for passing a list of images to view to generate links
            var dir = new DirectoryInfo(Server.MapPath(ServerConfigurator.imageStoragePath + User.Identity.Name + "/"));
            FileInfo[] fileNames = dir.GetFiles("*.*"); // list all files in personal directory 

            imagesDownloadDetails.items = new List<string>();
            imagesDownloadDetails.processedImageDataInJSON = null; // initialize with null. Null is treated as not-to-display in the view (when null - then it won't be displayed in the view)

            foreach (var file in fileNames)
            {
                imagesDownloadDetails.items.Add(file.Name); // saving names of images
                if (file.Name.StartsWith(ServerConfigurator.originalImageName))
                {
                    imagesDownloadDetails.originalImageExtension = file.Extension; // saving extension of original image. Extension is saved with dot at the beginning, i.e ".jpg"
                }
                if (file.Name.StartsWith(ServerConfigurator.processedImageName))
                {
                    imagesDownloadDetails.processedImageExtension = file.Extension; // saving extension of processed image. Extension is saved with dot at the beginning, i.e ".jpg"
                }
                if (file.Name.StartsWith(ServerConfigurator.afterProcessingDataFileName)) // read JSON file which is stored on the server to pass data into View
                {
                    StreamReader r = null;
                    try
                    {
                        r = new StreamReader(Server.MapPath(ServerConfigurator.imageStoragePath + User.Identity.Name + "/" + file));
                        string JSONFormattedData = r.ReadToEnd();
                        r.Close();
                        JsonConvert.DeserializeObject(JSONFormattedData); // try to deserialize JSON from file - if it is invalid - then exception will be thrown and "JSONFormattedData" string won't be passed into view
                        imagesDownloadDetails.processedImageDataInJSON = JSONFormattedData;                        
                    }
                    //catch (ArgumentException) { }
                    //catch (FileNotFoundException) { }
                    //catch (DirectoryNotFoundException) { }
                    //catch (IOException) { }
                    catch (OutOfMemoryException) { r.Close(); }
                    catch (Exception e) { r.Close(); }
                }
            }

            return imagesDownloadDetails;
        }
    }
}