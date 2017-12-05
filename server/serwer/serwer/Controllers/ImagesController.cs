using System;
using System.Web;
using System.Web.Mvc;
using System.IO;
using serwer.Models;
using serwer.Config;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using System.Threading;

namespace serwer.Controllers
{
    //[Authorize]
    public class ImagesController : Controller
    {
        // GET: Images
        public ActionResult Index()
        {
            return View();
        }

        [HttpGet]
        public ActionResult UploadFile()
        {
            UploadFileViewModel uploadFileViewModel = new UploadFileViewModel();

            string[] files = Directory.GetFiles(Server.MapPath("~/MatlabScripts/"));

            List<SelectListItem> list = new List<SelectListItem>();
            list.Add(new SelectListItem { Selected = true, Text = "Wybierz algorytm", Value = "-1" });

            foreach (var file in files)
            {
                string fileName = Path.GetFileNameWithoutExtension(file);
                list.Add(new SelectListItem { Selected = false, Text = fileName, Value = fileName });
            }

            uploadFileViewModel.algorithms = new SelectList(list);

            return View(uploadFileViewModel);
        }

        [HttpPost]
        [ValidateAntiForgeryToken]
        public ActionResult UploadFile(HttpPostedFileBase file, UploadFileViewModel model)
        {

            try
            {
                if (file.ContentLength > 0)
                {
                    // Threads will be used later for processing images in background
                    ThreadStart threadStart = new ThreadStart(testAsyn);
                    Thread thread = new Thread(threadStart);
                    thread.Start();

                    System.IO.DirectoryInfo di = new DirectoryInfo(Server.MapPath("~/Storage/" + User.Identity.Name +"/"));

                    foreach (FileInfo fileForDeletion in di.GetFiles())
                    {
                        fileForDeletion.Delete();
                    }

                    string user = User.Identity.Name; // fetches the user login, which is currently logged in. It is used to decide into which directory the image should be saved
                    
                    string fileName = Path.GetFileName(file.FileName); 
                    string storageDirectory = Server.MapPath("~/Storage/" + user + "/"); // directory into which we will save the original image

                    string processedFileName = ServerConfigurator.processedImageName + Path.GetExtension(file.FileName);
                    string originalFileName = ServerConfigurator.originalImageName + Path.GetExtension(file.FileName); // name of file, which is used to save the image on the server

                    string matlabScriptsDirectory = Server.MapPath("~/MatlabScripts/"); // in this directory are stored all matlab scripts

                    if (!System.IO.File.Exists(matlabScriptsDirectory + model.selectedAlgorithm + ".m"))
                    {
                        ViewBag.Message = "Błąd - nie ma takiego algorytmu na serwerze";
                        return View("imagesView", this.getImages());
                    }

                    string path = Path.Combine(storageDirectory, originalFileName);
                    file.SaveAs(path);

                    MLApp.MLApp matlab = new MLApp.MLApp();
                    matlab.Execute("cd " + matlabScriptsDirectory); // move Matlab shell context to the directory specified here    
                    object result = null; // output data
                    matlab.Feval(model.selectedAlgorithm, 0, out result, storageDirectory + originalFileName, storageDirectory + processedFileName);

                    matlab.Quit();

                }
                ViewBag.Message = "Pomyślnie przesłano obraz na serwer";
                return View("imagesView", this.getImages());
            }
            catch (Exception e)
            {
                ViewBag.Message = e.ToString();// "File upload failed!!";
                return View();
            }
        }

        private void testAsyn()
        {
            Thread.Sleep(10000);
            System.IO.Directory.CreateDirectory(Server.MapPath("~/Storage/testasync"));
        }

        [HttpGet]
        public ActionResult ImagesView()
        {
            return View(this.getImages());
        }

        [HttpGet]
        public FileResult Download(string ImageName)
        {
            return File(Server.MapPath("~/Storage/" + User.Identity.Name + "/" + ImageName), System.Net.Mime.MediaTypeNames.Application.Octet, ImageName);
        }

        // Saves all images names in directory to viewmodel object to retrieve them during displaying images in the view
        // There SHOULD ALWAYS be only two images per user on the server, but this method supports more, than two
        private ImagesDownloadDetails getImages()
        {
            ImagesDownloadDetails imagesDownloadDetails = new ImagesDownloadDetails();
            var dir = new DirectoryInfo(Server.MapPath("~/Storage/" + User.Identity.Name + "/"));
            FileInfo[] fileNames = dir.GetFiles("*.*");

            imagesDownloadDetails.items = new List<string>();

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
            }

            //var extensionInfo = from ims in imagesDownloadDetails.items where ims.StartsWith(ServerConfigurator.originalImageName + ".") || ims.StartsWith(ServerConfigurator.processedImageName + ".");
            return imagesDownloadDetails;
        }

        //private class NoCacheAttribute : ActionFilterAttribute
        //{
        //    public override void OnResultExecuting(ResultExecutingContext filterContext)
        //    {
        //        filterContext.HttpContext.Response.Cache.SetExpires(DateTime.UtcNow.AddDays(-1));
        //        filterContext.HttpContext.Response.Cache.SetValidUntilExpires(false);
        //        filterContext.HttpContext.Response.Cache.SetRevalidation(HttpCacheRevalidation.AllCaches);
        //        filterContext.HttpContext.Response.Cache.SetCacheability(HttpCacheability.NoCache);
        //        filterContext.HttpContext.Response.Cache.SetNoStore();

        //        base.OnResultExecuting(filterContext);
        //    }
        //}
    }
}