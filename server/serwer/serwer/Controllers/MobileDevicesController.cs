using Microsoft.AspNet.Identity;
using Microsoft.AspNet.Identity.Owin;
using Newtonsoft.Json;
using serwer.Config;
using serwer.Helpers;
using serwer.Models;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using System.Web;
using System.Web.Mvc;

namespace serwer.Controllers
{
    [Authorize]
    public class MobileDevicesController : Controller
    {
        [HttpPost]
        public String testToken(String email)
        {
            return "Test webd: " + email;
        }

        //[HttpPost]
        //public String testPost(LoginMobileViewModel loginMobileViewModel)
        //{
        //    return loginMobileViewModel.Email + ", " + loginMobileViewModel.Password;
        //}

        // checks if mobile app user with given email is logged in or not
        [HttpGet]
        [AllowAnonymous]
        public string checkIfLoggedIn(string token)
        {
            Dictionary<string, string> values = new Dictionary<string, string>();

            if (Request.IsAuthenticated)
            {
                values.Add("answer", "Yes");
                values.Add("guid", Guid.NewGuid().ToString());
                values.Add("username", User.Identity.Name);
                values.Add("username2", User.Identity.IsAuthenticated.ToString());
            }
            else
            {
                values.Add("answer", "Yes");
                values.Add("guid", Guid.NewGuid().ToString());
                values.Add("username", User.Identity.Name);
                values.Add("username2", User.Identity.IsAuthenticated.ToString());
            }

            return JsonConvert.SerializeObject(values);
        }


        [HttpPost]
        public HttpStatusCodeResult handleImageFromMobileApp()
        {
            try
            {
                String selectedAlgorithm = Request.Form.Get("selectedAlgorithm");
                if (String.IsNullOrEmpty(selectedAlgorithm) || Core.checkIfMatlabScriptExistsOnServer(Server.MapPath(ServerConfigurator.matlabScriptsPath), selectedAlgorithm))
                {
                    HttpPostedFileBase file = Request.Files[0];
                    file.SaveAs(Server.MapPath("~/Mobile/" + User.Identity.Name + "original" + Path.GetExtension(file.FileName)));
                    Core.startProcessingImage(file, User.Identity.Name, selectedAlgorithm);
                    return new HttpStatusCodeResult(HttpStatusCode.OK);
                }                
            }
            catch(Exception e)
            {
                System.Diagnostics.Debug.WriteLine(e);
                return new HttpStatusCodeResult(HttpStatusCode.InternalServerError);
            }

            return new HttpStatusCodeResult(HttpStatusCode.BadRequest);            
        }

        [HttpPost]
        public HttpStatusCodeResult GetFileFromDisk()
        {
            string user = User.Identity.Name;

            try
            {
                string fileToDownload = new DirectoryInfo(Server.MapPath(ServerConfigurator.imageStoragePath + user + "/"))
                                                        .GetFiles()
                                                        .Select(s => s.Name)
                                                        .Single(s => s.StartsWith("processed"));
                //HttpContext.Response.Write("sdfgsdfg");
                HttpContext.Response.WriteFile(Server.MapPath(ServerConfigurator.imageStoragePath + user + "/" + fileToDownload)); // append file to content body

                return new HttpStatusCodeResult(HttpStatusCode.OK);
            }
            catch(Exception e)
            {
                Debug.WriteLine(e);
            }

            return new HttpStatusCodeResult(HttpStatusCode.BadRequest);            
        }

        [HttpPost]
        public string getAlgorithms()
        {
            Dictionary<string, string> dict = new Dictionary<string, string>();
            string[]  files = Directory.GetFiles(Server.MapPath(ServerConfigurator.matlabScriptsPath));
            int counter = 0;
            foreach(string file in files)
            {                
                dict.Add(counter.ToString(), Path.GetFileNameWithoutExtension(file));
                counter++;
            }
            return JsonConvert.SerializeObject(dict); 
        }
        
        [HttpGet]
        public void getData()
        {
            HttpContext.Response.Headers.Add("s", "d");
            
            StreamReader streamReader = new StreamReader(Server.MapPath(ServerConfigurator.imageStoragePath + User.Identity.Name + "/" + "processingResults.json"));
            //string s = streamReader.ReadLine();
            //HttpContext.Response.Write(streamWriter.ToString());
            HttpContext.Response.Write(streamReader.ReadLine());
            streamReader.Close();
        }
    }
}