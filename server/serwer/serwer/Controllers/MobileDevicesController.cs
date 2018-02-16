using Microsoft.AspNet.Identity;
using Microsoft.AspNet.Identity.Owin;
using Newtonsoft.Json;
using serwer.Helpers;
using serwer.Models;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using System.Web;
using System.Web.Mvc;

namespace serwer.Controllers
{
    public class MobileDevicesController : Controller
    {
        //private readonly ApplicationDbContext context;

        //private ApplicationSignInManager _signInManager;
        //private ApplicationUserManager _userManager;

        //public MobileDevicesController()
        //{

        //}

        //public MobileDevicesController(ApplicationUserManager userManager, ApplicationSignInManager signInManager)
        //{
        //    UserManager = userManager;
        //    SignInManager = signInManager;
        //}

        //public ApplicationSignInManager SignInManager
        //{
        //    get
        //    {
        //        return _signInManager ?? HttpContext.GetOwinContext().Get<ApplicationSignInManager>();
        //    }
        //    private set
        //    {
        //        _signInManager = value;
        //    }
        //}

        //public ApplicationUserManager UserManager
        //{
        //    get
        //    {
        //        return _userManager ?? HttpContext.GetOwinContext().GetUserManager<ApplicationUserManager>();
        //    }
        //    private set
        //    {
        //        _userManager = value;
        //    }
        //}


        // GET: MobileDevices
        public ActionResult Index()
        {
            return View();
        }

        [Authorize]
        [HttpPost]
        public String testToken(String email)
        {
            return "Test webd: " + email;
        }

        [HttpPost]
        public String testPost(LoginMobileViewModel loginMobileViewModel)
        {
            return loginMobileViewModel.Email + ", " + loginMobileViewModel.Password;
        }

        // checks if mobile app user with given email is logged in or not
        [HttpPost]
        public string checkIfLoggedIn(LoginCheck loginCheck)
        {
            Dictionary<string, string> values = new Dictionary<string, string>();
            if (loginCheck.Email == null || loginCheck.Token == null)
            {
                values.Add("login", "no");
                return JsonConvert.SerializeObject(values);
            }

            string token = "sadjifhh08934242utrrhhfgds8v034775q29t9ftfjhds8gvb";

            // 1. Check if user with given email exists in users registry
            // 2. Check if he is logged in

            values.Add("Email", loginCheck.Email);

            if (loginCheck.Token.Equals(token))
                values.Add("login", "yes");
            else
                values.Add("login", "no");

            return JsonConvert.SerializeObject(values);
        }


        [HttpPost]
        public string handleImageFromMobileApp()
        {
            var c = Request.Files;
            try
            {
                Request.Files[0].SaveAs(Server.MapPath("~/Mobile/output.jpg"));
            }catch(Exception e)
            {
                System.Diagnostics.Debug.WriteLine(e);
            }
            //Dictionary<string, string> response = new Dictionary<string, string>();
            //if(handleImage.Email == null || handleImage.Token == null || handleImage.Image == null || handleImage.ImageHash == null)
            //{
            //    response.Add("correctData", "no");
            //    return JsonConvert.SerializeObject(response);
            //}

            //string hash = Mobile.sha256(handleImage.Image);
            //System.Diagnostics.Debug.WriteLine(hash);
            //response.Add("ImageHash", Mobile.sha256(handleImage.Image));

            //Image image = Mobile.Base64ToImage(handleImage.Image);
            //image.Save(Server.MapPath("~/Mobile/output.jpg"), ImageFormat.Jpeg);

            return "df";
        }

        public FilePathResult GetFileFromDisk()
        {
            return File(Server.MapPath("~/Mobile/output.jpg"), "multipart/form-data", "costam.jpg");
        }

        //public HttpResponseMessage testReturn()
        //{
        //    HttpResponseMessage http = new HttpResponseMessage();
        //    http.Content = new HandleImage() { Email = "d", Image = "df", ImageHash = "dsfgdfsgsgf", Token = "sdfg" };
        //    return Request.CreateResponse(HttpStatusCode.OK, new HandleImage() { Email = "d", Image = "df", ImageHash = "dsfgdfsgsgf", Token = "sdfg" });
        //}


        //// POST api/mobilelogin
        //[AllowAnonymous]
        //[HttpPost]
        //public async Task<ActionResult> Login(LoginMobileResource model)
        //{
        //    if (ModelState.IsValid)
        //    {
        //        // This doesn't count login failures towards account lockout
        //        // To enable password failures to trigger account lockout, set lockoutOnFailure: true
        //        var result = await _signInManager.PasswordSignInAsync(model.Email, model.Password, model.RememberMe, shouldLockout: false);
        //        if (result == Microsoft.AspNet.Identity.Owin.SignInStatus.Success)
        //        {
        //            var user = context.Users.SingleOrDefault(c => c.Email == model.Email);
        //            /* _logger.LogInformation(1, "User logged in.");
        //             return RedirectToLocal(returnUrl);*/
        //            var modeltoreturn = new LoginMobileResource
        //            {
        //                Status = "Ok",
        //                Email = user.Email,
        //                Password = "",
        //                RememberMe = model.RememberMe,
        //                UserId = user.Id
        //            };
        //            return View(modeltoreturn);
        //        }
        //        else
        //        {
        //            var modeltoreturn = new LoginMobileResource
        //            {
        //                Status = "Wrong password",
        //                Email = model.Email,
        //                Password = "",
        //                UserId = ""
        //            };
        //            return View(modeltoreturn);
        //        }
        //    }
        //    // If we got this far, something failed, redisplay form
        //    model.Status = "Non-existent account";
        //    return View(model);
        //}
    }
}