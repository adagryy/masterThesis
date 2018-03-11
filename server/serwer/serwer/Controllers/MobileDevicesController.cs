using Newtonsoft.Json;
using serwer.Config;
using serwer.Helpers;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net;
using System.Web;
using System.Web.Mvc;
using System.Web.Security;

namespace serwer.Controllers
{
    // Class for custom management of unauthorized requests
    public class MobileAuthorize : AuthorizeAttribute
    {
        public override void OnAuthorization(AuthorizationContext filterContext)
        {
            // Actions with "AllowAnonymous attribute will be allowed withoud authentication
            if (filterContext.ActionDescriptor.IsDefined(typeof(AllowAnonymousAttribute), true) || filterContext.ActionDescriptor.ControllerDescriptor.IsDefined(typeof(AllowAnonymousAttribute), true))
            {
                return;
            }

            // If they are authorized, handle accordingly
            if (this.AuthorizeCore(filterContext.HttpContext))
            {
                base.OnAuthorization(filterContext);
            }
            else
            {
                // Otherwise redirect to your specific authorized area
                filterContext.Result = new RedirectResult("~/Account/UnAuthorized");
            }
        }
    }

    [MobileAuthorize]
    public class MobileDevicesController : Controller
    {
        //[HttpGet]
        //public String testToken(String email)
        //{
        //    return "Test webd: " + email;
        //}


        //// checks if mobile app user with given email is logged in or not
        //[HttpGet]
        //[AllowAnonymous]
        //public  string checkIfLoggedIn(string token)
        //{
        //    Dictionary<string, string> values = new Dictionary<string, string>();

        //    if (Request.IsAuthenticated)
        //    {
        //        values.Add("answer", "Yes");
        //        values.Add("guid", Guid.NewGuid().ToString());
        //        values.Add("username", User.Identity.Name);
        //        values.Add("username2", User.Identity.IsAuthenticated.ToString());
        //    }
        //    else
        //    {
        //        values.Add("answer", "Yes");
        //        values.Add("guid", Guid.NewGuid().ToString());
        //        values.Add("username", User.Identity.Name);
        //        values.Add("username2", User.Identity.IsAuthenticated.ToString());
        //    }

        //    return JsonConvert.SerializeObject(values);
        //}

        [HttpPost]
        [AllowAnonymous]
        public void checkIfMobileAppLoggedIn()
        {
            if (User.Identity.IsAuthenticated)
            {
                HttpContext.Response.Write("You are ok :)");
                HttpContext.Response.StatusCode = (int) HttpStatusCode.OK;
            }
            else
            {
                HttpContext.Response.Write("Fuck you!");
                HttpContext.Response.StatusCode = (int) HttpStatusCode.Forbidden;
            }
        }

        // Handle image from mobile app
        [HttpPost]
        public HttpStatusCodeResult handleImageFromMobileApp()
        {
            try
            {
                String selectedAlgorithm = Request.Form.Get("selectedAlgorithm"); // gets selected algorithm for processing
                if (String.IsNullOrEmpty(selectedAlgorithm) || Core.checkIfMatlabScriptExistsOnServer(Server.MapPath(ServerConfigurator.matlabScriptsPath), selectedAlgorithm))
                {
                    HttpPostedFileBase file = Request.Files[0];
                    Core.startProcessingImage(file, User.Identity.Name, selectedAlgorithm); // start exact processing
                    return new HttpStatusCodeResult(HttpStatusCode.OK);
                }                
            }
            catch(Exception e)
            {
                Debug.WriteLine(e);
                return new HttpStatusCodeResult(HttpStatusCode.InternalServerError);
            }

            return new HttpStatusCodeResult(HttpStatusCode.BadRequest);            
        }

        // This action returns an image for Mobile app client
        [HttpPost]
        public HttpStatusCodeResult GetFileFromDisk()
        {
            string user = User.Identity.Name;

            try
            {
                // finds file, which should be returned to the Mobile app client
                string fileToDownload = new DirectoryInfo(Server.MapPath(ServerConfigurator.imageStoragePath + user + "/"))
                                                        .GetFiles()
                                                        .Select(s => s.Name)
                                                        .Single(s => s.StartsWith("processed"));

                HttpContext.Response.WriteFile(Server.MapPath(ServerConfigurator.imageStoragePath + user + "/" + fileToDownload)); // append file to content body
                return new HttpStatusCodeResult(HttpStatusCode.OK);
            }catch(InvalidOperationException e)
            {

            }
            catch(ArgumentNullException e) { }
            catch (Exception e) { }

            return new HttpStatusCodeResult(HttpStatusCode.BadRequest);            
        }

        // Returns a list of available algorithms to the Mobile App Client
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
        
        // Returns data about processed image, which are generated by Matlab scripts in JSON format
        [HttpPost]
        public void getData()
        {
            HttpContext.Response.Headers.Add("s", "d"); // unused
            try
            {
                StreamReader streamReader = new StreamReader(Server.MapPath(ServerConfigurator.imageStoragePath + User.Identity.Name + "/" + "processingResults.json"));                
                HttpContext.Response.Write(streamReader.ReadLine());
                streamReader.Close();
            }
            catch(FileNotFoundException e) { }
            catch(Exception e) { }
            
        }
    }


}