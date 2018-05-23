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
using System.Text.RegularExpressions;
using System.Drawing;

namespace serwer.Controllers
{
    [Authorize]
    public class ImagesController : Controller
    {
        // This action represents a view for image uploading
        [HttpGet]
        public ActionResult UploadFile()
        {
            UploadFileViewModel uploadFileViewModel = new UploadFileViewModel(); // ViewModel for passing a list of available matlab algorithms on the server to the client

            //string[] files = Directory.GetFiles(Server.MapPath(ServerConfigurator.matlabScriptsPath)); // Read all matlab algorithms available on server
            string[] files = Directory.GetFiles(ServerConfigurator.matlabScripts); // Read all matlab algorithms available on server

            //List<SelectListItem> list = new List<SelectListItem>(); // create list of algorithms later passed to the view
            //list.Add(new SelectListItem { Selected = true, Text = "Wybierz algorytm", Value = "-1" }); // default (first) value "Wybierz algorytm"

            //foreach (var file in files) // add all algorithm to the list
            //{
            //    string fileName = Path.GetFileNameWithoutExtension(file);
            //    list.Add(new SelectListItem { Selected = false, Text = fileName, Value = fileName });
            //}

            //uploadFileViewModel.algorithms = new SelectList(list); // save list to the viewmodel "SelectList" object

            Dictionary<string, string> list = new Dictionary<string, string>(); // create list of algorithms later passed to the view
            //list.Add(new SelectListItem { Selected = true, Text = "Wybierz algorytm", Value = "-1" }); // default (first) value "Wybierz algorytm"

            foreach (var file in files) // add all algorithm to the list
            {
                string fileName = Path.GetFileNameWithoutExtension(file);
                list.Add(fileName, fileName);
            }

            uploadFileViewModel.algorithms = list; // save list to the viewmodel "SelectList" object

            return View(uploadFileViewModel);
        }

        // This action handles a HTTP POST request with image uploaded
        [HttpPost]
        [ValidateAntiForgeryToken]
        public ActionResult UploadFile(HttpPostedFileBase file, UploadFileViewModel model)
        {
            try
            {
                // Check if file (image) is: not null, not empty and has an extension (lower case) which is supported by server (checked by regex)
                if(Core.checkIfReceivedFileHasValidExtension(file))
                {
                    Image.FromStream(file.InputStream); // Try to create Image - if "file" is not valid - then Image creation fails and exception will be thrown

                    string user = User.Identity.Name; // fetches the user login, which is currently logged in. It is used to decide into which directory the image should be saved
                                       
                    if (!Core.checkIfMatlabScriptExistsOnServer(ServerConfigurator.matlabScripts, model.selectedAlgorithm)) // Check if given algorithm exists on the server
                    {
                        ViewBag.Message = "Błąd - nie ma takiego algorytmu na serwerze";
                        return View("imagesView", this.getImages());
                    }

                    Core.startProcessingImage(file, user, model.selectedAlgorithm); // starts exact image processing. To start image processing we need image 
                                                                                    // to be processed (file), which user requested processing (user) and which algorithm 
                                                                                    // to use for processing (model.selectedAlgorithm)
                    ViewBag.Message = "Pomyślnie przesłano obraz na serwer";
                    return RedirectToAction("imagesView");
                }
                else
                {
                    // Invalid image received from client
                    return RedirectToAction("UploadFile");
                }                
            }
            catch (Exception e)
            {
                ViewBag.Message = "Error during sending image to server";
                return RedirectToAction("UploadFile");
            }
        }

        [HttpGet]
        public ActionResult ImagesView()
        {
            return View(this.getImages());
        }

        // Returns an image which is out of the server webroot
        [HttpGet]
        public FileResult DownloadImage(string ImageName)
        {
            try
            {
                return File(ServerConfigurator.usersStorage + User.Identity.Name + ServerConfigurator.directoryPathSeparator + ImageName, System.Net.Mime.MediaTypeNames.Application.Octet, ImageName);
            }
            catch (Exception)
            {
                return null;
            }
        }


        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        /// </summary>
        /// <returns></returns>
        // In final version I have removed feature of removing images from server

        //[HttpPost]
        //[ValidateAntiForgeryToken]
        //public ActionResult RemoveImage(ImagesDownloadDetails imagesDownloadDetails)
        //{
        //    // Using regular expression check if correct filename was received from client for removing image
        //    string pattern = "(^" + ServerConfigurator.originalImageName + "\\." + ServerConfigurator.supportedImageExtensions + "$)|(^" + ServerConfigurator.processedImageName + "\\." + ServerConfigurator.supportedImageExtensions + "$)";

        //    if (!Regex.Match(imagesDownloadDetails.removeImageName, pattern).Success)
        //        return RedirectToAction("ImagesView");

        //    try
        //    {
        //        //System.IO.File.Delete(Server.MapPath(imagesDownloadDetails.removeImagePath));
        //        System.IO.File.Delete(ServerConfigurator.usersStorage + User.Identity.Name + ServerConfigurator.directoryPathSeparator + imagesDownloadDetails.removeImageName);
        //    }
        //    catch (ArgumentException) { }
        //    //catch (ArgumentNullException) { } // 
        //    catch (DirectoryNotFoundException) { }
        //    catch (IOException) { }
        //    catch (NotSupportedException) { }
        //    //catch (PathTooLongException) { } // already catched by IOException
        //    catch (UnauthorizedAccessException) { }
        //    catch (Exception) { }

        //    return RedirectToAction("ImagesView");
        //}
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // Saves all images names in directory to viewmodel object to retrieve them during displaying images in the view named "ImagesView"
        // There SHOULD ALWAYS be only two images per user on the server, but this method supports more, than two
        private ImagesDownloadDetails getImages()
        {
            ImagesDownloadDetails imagesDownloadDetails = new ImagesDownloadDetails(); // New ViewModel object for passing a list of images to view to generate links
            imagesDownloadDetails.items = new List<string>();
            imagesDownloadDetails.processedImageDataInJSON = null; // initialize with null. Null is treated as not-to-display in the view (when null - then it won't be displayed in the view)

            //var dir = new DirectoryInfo(Server.MapPath(ServerConfigurator.imageStoragePath + User.Identity.Name + "/"));
            var dir = new DirectoryInfo(ServerConfigurator.usersStorage + User.Identity.Name + ServerConfigurator.directoryPathSeparator);
            FileInfo[] fileNames = null;
            try
            {
                fileNames = dir.GetFiles("*.*"); // list all files in personal directory 
            }catch(DirectoryNotFoundException) // when directory not exists
            {
                return imagesDownloadDetails;
            }            

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
                        //r = new StreamReader(Server.MapPath(ServerConfigurator.imageStoragePath + User.Identity.Name + "/" + file));
                        r = new StreamReader(ServerConfigurator.usersStorage + User.Identity.Name + ServerConfigurator.directoryPathSeparator + file);
                        string JSONFormattedData = r.ReadToEnd();
                        r.Close();
                        JsonConvert.DeserializeObject(JSONFormattedData); // try to deserialize JSON from file - if it is invalid - then exception will be thrown and "JSONFormattedData" string won't be passed into view
                        imagesDownloadDetails.processedImageDataInJSON = JSONFormattedData;                        
                    }
                    catch (OutOfMemoryException) { r.Close(); }
                    catch (Exception) { r.Close(); }
                }
            }

            return imagesDownloadDetails;
        }
    }
}