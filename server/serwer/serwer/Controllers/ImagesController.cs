using System;
using System.Web;
using System.Web.Mvc;
using System.IO;

namespace serwer.Controllers
{
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
            return View();
        }

        [HttpPost]
        public ActionResult UploadFile(HttpPostedFileBase file)
        {
            try
            {
                if (file.ContentLength > 0)
                {
                    string hardcodedImageFileName = "images.jpg";
                    string _FileName = Path.GetFileName(file.FileName);
                    string _path = Path.Combine(Server.MapPath("~/Storage"), hardcodedImageFileName);
                    file.SaveAs(_path);

                    MLApp.MLApp matlab = new MLApp.MLApp();

                    matlab.Execute(@"cd c:\Users\grycz\Desktop\masterThesis\server\serwer\serwer\MatlabScripts"); // move Matlab shell context to the directory specified here
                    string storageDirectory = "C:\\Users\\grycz\\Desktop\\masterThesis\\server\\serwer\\serwer\\Storage\\";
                    string processedImageName = "negative.jpg";

                    object result = null; // output data
                    matlab.Feval("imageNegative", 0, out result, storageDirectory + hardcodedImageFileName, storageDirectory + processedImageName);

                }
                ViewBag.Message = "File Uploaded Successfully!!";
                return View();
            }
            catch(Exception e)
            {
                Console.WriteLine("BŁĄD");
                Console.WriteLine();
                Console.WriteLine(e);
                ViewBag.Message = "File upload failed!!";

                return View();
            }
        }
    }
}