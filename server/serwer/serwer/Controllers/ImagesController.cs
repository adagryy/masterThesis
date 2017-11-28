using System;
using System.Web;
using System.Web.Mvc;
using System.IO;
using serwer.Models;

namespace serwer.Controllers
{
    [Authorize]
    public class ImagesController : Controller
    {
        // GET: Images
        public ActionResult Index()
        {
            UploadFileViewModel uploadFileViewModel = new UploadFileViewModel();
            uploadFileViewModel.algorithms.Add("test");
            return View(uploadFileViewModel);
        }

        [HttpGet]
        [OutputCache(NoStore = true, Duration = 0)] // disables cache for this specific action
        public ActionResult UploadFile()
        {

            return View();
        }

        [HttpPost]
        [OutputCache(NoStore = true, Duration = 0)] // disables cache for this specific action
        [ValidateAntiForgeryToken]
        public ActionResult UploadFile(HttpPostedFileBase file)
        {
            try
            {
                if (file.ContentLength > 0)
                {
                    string user = User.Identity.Name;
                    string hardcodedImageFileName = "images.jpg";
                    string fileName = Path.GetFileName(file.FileName);
                    string storageDirectory = Server.MapPath("~/Storage/");
                    string processedImageName = "negative.jpg";
                    string matlabScriptsDirectory = Server.MapPath("~/MatlabScripts/");

                    string path = Path.Combine(storageDirectory, hardcodedImageFileName);
                    file.SaveAs(path);

                    MLApp.MLApp matlab = new MLApp.MLApp();
                    matlab.Execute("cd " + matlabScriptsDirectory); // move Matlab shell context to the directory specified here    
                    object result = null; // output data
                    matlab.Feval("imageNegative", 0, out result, storageDirectory + hardcodedImageFileName, storageDirectory + processedImageName);

                    matlab.Quit();

                }
                ViewBag.Message = "Pomyślnie przesłano obraz na serwer";
                return View();
            }
            catch(Exception e)
            {
                ViewBag.Message = e.ToString();// "File upload failed!!";
                return View();
            }
        }
    }
}