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

        // Using this method you can check from your mobile app if you are logged in or not
        // It sets 200 Http code if your mobile client is authenticated and 403 Http code otherwise
        [HttpPost]
        [AllowAnonymous]
        public void checkIfMobileAppLoggedIn()
        {
            HttpContext.Response.TrySkipIisCustomErrors = true; // prevent IIS from displaying error pages for non-OK Http codes
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

        // Handle image from mobile app. Send POST request with image and selected algorithm to this method from mobile app.
        [HttpPost]
        public HttpStatusCodeResult handleImageFromMobileApp()
        {
            HttpContext.Response.TrySkipIisCustomErrors = true; // prevent IIS from displaying error pages for non-OK Http codes
            try
            {
                String selectedAlgorithm = Request.Form.Get("selectedAlgorithm"); // gets selected algorithm for processing
                // Check if selected algorithm is not null or ampty and then check if selected algorithm exists on the server
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
                return new HttpStatusCodeResult(HttpStatusCode.InternalServerError); // Error during processing
            }

            // Incorrect data received from client (no image, no selectedalgorithm, selectedalgorithm does not exist). Bad reqest (400 Http)
            return new HttpStatusCodeResult(HttpStatusCode.BadRequest);            
        }

        // This action returns a processed image for Mobile app client
        [HttpPost]
        public HttpStatusCodeResult GetFileFromDisk()
        {
            HttpContext.Response.TrySkipIisCustomErrors = true; // prevent IIS from displaying error pages for non-OK Http codes
            // Fetch username of user, which requests image for download.
            // Username is needed for determine user-specific directory for client
            string user = User.Identity.Name;

            try
            {
                // finds file, which should be returned to the Mobile app client
                string fileToDownload = new DirectoryInfo(Server.MapPath(ServerConfigurator.imageStoragePath + user + "/"))
                                                        .GetFiles()
                                                        .Select(s => s.Name)
                                                        .Single(s => s.StartsWith("processed"));

                HttpContext.Response.WriteFile(Server.MapPath(ServerConfigurator.imageStoragePath + user + "/" + fileToDownload)); // append file to response content body
                return new HttpStatusCodeResult(HttpStatusCode.OK);
            }catch(InvalidOperationException)
            {
                HttpContext.Response.Write("Processing not finished yet!");
                return new HttpStatusCodeResult(HttpStatusCode.NotFound);
            }
            catch(ArgumentNullException) { }
            catch (Exception) { }
            // Something went wrong (image is not processed yet). Bad request (400 Http). Maybe it should be set to 404?
            return new HttpStatusCodeResult(HttpStatusCode.BadRequest);            
        }

        // Returns a list of available algorithms to the Mobile App Client
        [HttpPost]
        public string getAlgorithms()
        {
            // Algorithms will be read from servers' disk into this dictionary in the following order: keys are 
            // For instance if you have four algorithms on the server: imageNegative, imageRotater, imageBlurrer and imageCropper on the server, then this dictionary will be following (keys are consecutive natural numbers starting from zero, and values are algorithms names):
            // 0: imageNegative
            // 1: imageRotater
            // 2: imageBlurrer
            // 3: imageCropper
            // Of course this dictionary is created in foreach loop below and will be serialized into JSON format by the last line ("JsonConvert.SerializeObject(dict);") 
            // of this method into such a body:
            // {"0":"imageNegative","1":"imageRotater","2":"imageBlurrer","3":"imageCropper"}

            HttpContext.Response.TrySkipIisCustomErrors = true; // skip IIS default error pages like 404, 500 etc

            Dictionary<string, string> dict = new Dictionary<string, string>(); 
            string[]  files = Directory.GetFiles(Server.MapPath(ServerConfigurator.matlabScriptsPath)); // read files names (matlab scripts) into string array, then iterate over them.
            int counter = 0; // counter for numbering dict keys
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
            HttpContext.Response.TrySkipIisCustomErrors = true; // skip IIS default error pages like 404, 500 etc
            try
            {
                // Stream for reading file with JSON-formatted data about processed image
                StreamReader streamReader = new StreamReader(Server.MapPath(ServerConfigurator.imageStoragePath + User.Identity.Name + "/" + "processingResults.json"));                
                HttpContext.Response.Write(streamReader.ReadLine()); // All data are in the first line
                streamReader.Close();
            }
            catch(FileNotFoundException) {
                HttpContext.Response.Write("Processing not finished yet!");
                HttpContext.Response.StatusCode = (int) (HttpStatusCode.NotFound);
            }
            catch(Exception) { }            
        }
    }


}