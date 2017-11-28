using System;
using System.Web;
using System.Web.Mvc;
using System.IO;
using serwer.Models;
using System.Collections.Generic;

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

        [HttpGet]
        public ActionResult UploadFile()
        {
            UploadFileViewModel uploadFileViewModel = new UploadFileViewModel();

            string[] files = Directory.GetFiles(Server.MapPath("~/MatlabScripts/"));

            List<SelectListItem> list = new List<SelectListItem>();        
            list.Add(new SelectListItem { Selected = true, Text = "Wybierz algorytm", Value = "-1"});

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
                    string user = User.Identity.Name;
                    string hardcodedImageFileName = "images.jpg";
                    string fileName = Path.GetFileName(file.FileName);
                    string storageDirectory = Server.MapPath("~/Storage/" + user + "/");
                    string processedImageName = "negative.jpg";
                    string matlabScriptsDirectory = Server.MapPath("~/MatlabScripts/");

                    if (!System.IO.File.Exists(matlabScriptsDirectory + model.selectedAlgorithm + ".m"))
                    {
                        ViewBag.Message = "Błąd - nie ma takiego algorytmu na serwerze";
                        return View("imagesView");
                    }

                    string path = Path.Combine(storageDirectory, hardcodedImageFileName);
                    file.SaveAs(path);

                    MLApp.MLApp matlab = new MLApp.MLApp();
                    matlab.Execute("cd " + matlabScriptsDirectory); // move Matlab shell context to the directory specified here    
                    object result = null; // output data
                    matlab.Feval(model.selectedAlgorithm, 0, out result, storageDirectory + hardcodedImageFileName, storageDirectory + processedImageName);

                    matlab.Quit();

                }
                ViewBag.Message = "Pomyślnie przesłano obraz na serwer";
                return View("imagesView");
            }
            catch(Exception e)
            {
                ViewBag.Message = e.ToString();// "File upload failed!!";
                return View();
            }
        }

        [HttpGet]
        public ActionResult ImagesView()
        {
            return View();
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