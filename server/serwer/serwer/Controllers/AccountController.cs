using System;
using System.Globalization;
using System.Linq;
using System.Security.Claims;
using System.Threading.Tasks;
using System.Web;
using System.Web.Mvc;
using Microsoft.AspNet.Identity;
using Microsoft.AspNet.Identity.Owin;
using Microsoft.Owin.Security;
using serwer.Models;
using System.IO;
using System.Threading;
using System.Net;
using serwer.Config;
using System.Web.Security;
using System.Collections.Generic;

namespace serwer.Controllers
{
    [Authorize]
    public class AccountController : Controller
    {
        private ApplicationSignInManager _signInManager;
        private ApplicationUserManager _userManager;

        public AccountController()
        {
        }

        public AccountController(ApplicationUserManager userManager, ApplicationSignInManager signInManager)
        {
            UserManager = userManager;
            SignInManager = signInManager;
        }

        public ApplicationSignInManager SignInManager
        {
            get
            {
                return _signInManager ?? HttpContext.GetOwinContext().Get<ApplicationSignInManager>();
            }
            private set
            {
                _signInManager = value;
            }
        }

        public ApplicationUserManager UserManager
        {
            get
            {
                return _userManager ?? HttpContext.GetOwinContext().GetUserManager<ApplicationUserManager>();
            }
            private set
            {
                _userManager = value;
            }
        }

        //
        // GET: /Account/Login
        [AllowAnonymous]
        public ActionResult Login(string returnUrl)
        {
            ViewBag.ReturnUrl = returnUrl;
            return View();
        }

        [AllowAnonymous]
        public HttpStatusCodeResult UnAuthorized(string message)
        {
            HttpContext.Response.TrySkipIisCustomErrors = true; // skip IIS default error pages like 404, 500 etc
            HttpContext.Response.Write("unauthorized"); // content when unauthorized request reaches server
            return new HttpStatusCodeResult(HttpStatusCode.Forbidden); // Access denied
        }

        //
        // POST: /Account/Login
        [HttpPost]
        [AllowAnonymous]
        [ValidateAntiForgeryToken]
        public async Task<ActionResult> Login(LoginViewModel model, string returnUrl)
        {
            if (!ModelState.IsValid)
            {
                return View(model);
            }

            var user = UserManager.FindByEmail(model.Email); // find the user associated with an email
            if (user == null)
            {
                ModelState.AddModelError("", "Błąd logowania. Podany adres email nie został znaleziony w systemie");
                return View(model);
            }
            // This doesn't count login failures towards account lockout
            // To enable password failures to trigger account lockout, change to shouldLockout: true
            var result = await SignInManager.PasswordSignInAsync(user.UserName, model.Password, model.RememberMe, shouldLockout: false);
            switch (result)
            {
                case SignInStatus.Success:
                    return RedirectToLocal(returnUrl);
                case SignInStatus.LockedOut:
                    return View("Lockout");
                case SignInStatus.RequiresVerification:
                    return RedirectToAction("SendCode", new { ReturnUrl = returnUrl, RememberMe = model.RememberMe });
                case SignInStatus.Failure:
                default:
                    ModelState.AddModelError("", "Błąd logowania.");
                    return View(model);
            }
        }

        [HttpPost]
        [AllowAnonymous]
        public async Task<HttpStatusCodeResult> MobileLogin(LoginViewModel model)
        {
            ApplicationUser applicationUser = UserManager.FindByEmail(model.Email); // try to find user in database

            if (!(applicationUser is null)) // if user was found...
            {
                SignInStatus signInStatus = await SignInManager.PasswordSignInAsync(applicationUser.UserName, model.Password, model.RememberMe, shouldLockout: false); // try to sign the user in using password received from client

                switch (signInStatus) // signInStatus - object containing information about signing in status
                {
                    case SignInStatus.Success:
                        return new HttpStatusCodeResult(HttpStatusCode.OK); // sign in successful
                    case SignInStatus.LockedOut:
                        return new HttpStatusCodeResult(HttpStatusCode.Forbidden); // account locked
                    case SignInStatus.RequiresVerification:
                        return new HttpStatusCodeResult(HttpStatusCode.Forbidden); // unverified account
                    case SignInStatus.Failure:
                        return new HttpStatusCodeResult(HttpStatusCode.Forbidden); // sing in error (wrong password)
                    default:
                        return new HttpStatusCodeResult(HttpStatusCode.Forbidden); // other sign in error
                }
            }

            return new HttpStatusCodeResult(HttpStatusCode.Forbidden); // user not found in the database

        }

        //
        // GET: /Account/VerifyCode
        [AllowAnonymous]
        public async Task<ActionResult> VerifyCode(string provider, string returnUrl, bool rememberMe)
        {
            // Require that the user has already logged in via username/password or external login
            if (!await SignInManager.HasBeenVerifiedAsync())
            {
                return View("Error");
            }
            return View(new VerifyCodeViewModel { Provider = provider, ReturnUrl = returnUrl, RememberMe = rememberMe });
        }

        //
        // POST: /Account/VerifyCode
        [HttpPost]
        [AllowAnonymous]
        [ValidateAntiForgeryToken]
        public async Task<ActionResult> VerifyCode(VerifyCodeViewModel model)
        {
            if (!ModelState.IsValid)
            {
                return View(model);
            }

            // The following code protects for brute force attacks against the two factor codes. 
            // If a user enters incorrect codes for a specified amount of time then the user account 
            // will be locked out for a specified amount of time. 
            // You can configure the account lockout settings in IdentityConfig
            var result = await SignInManager.TwoFactorSignInAsync(model.Provider, model.Code, isPersistent: model.RememberMe, rememberBrowser: model.RememberBrowser);
            switch (result)
            {
                case SignInStatus.Success:
                    return RedirectToLocal(model.ReturnUrl);
                case SignInStatus.LockedOut:
                    return View("Lockout");
                case SignInStatus.Failure:
                default:
                    ModelState.AddModelError("", "Niepoprawny kod.");
                    return View(model);
            }
        }

        //
        // GET: /Account/Register
        [Authorize(Roles = ServerConfigurator.adminRole)]
        public ActionResult Register()
        {
            return View();
        }

        //
        // POST: /Account/Register
        [HttpPost]
        [Authorize(Roles = ServerConfigurator.adminRole)]
        [ValidateAntiForgeryToken]
        public async Task<ActionResult> Register(RegisterViewModel model)
        {
            if (ModelState.IsValid)
            {
                var user = new ApplicationUser { Firstname = model.Firstname, Lastname = model.Lastname, UserName = model.UserName, Email = model.Email };

                var result = await UserManager.CreateAsync(user, model.Password);
                if (result.Succeeded)
                {
                    //await SignInManager.SignInAsync(user, isPersistent:false, rememberBrowser:false);

                    string storageDirectory = Server.MapPath("~/Storage/" + model.UserName);

                    if (!Directory.Exists(storageDirectory)) // if personal directory for every user does not exists, here it is created
                    {
                        Directory.CreateDirectory(storageDirectory);
                    }

                    // For more information on how to enable account confirmation and password reset please visit https://go.microsoft.com/fwlink/?LinkID=320771
                    // Send an email with this link
                    // string code = await UserManager.GenerateEmailConfirmationTokenAsync(user.Id);
                    // var callbackUrl = Url.Action("ConfirmEmail", "Account", new { userId = user.Id, code = code }, protocol: Request.Url.Scheme);
                    // await UserManager.SendEmailAsync(user.Id, "Confirm your account", "Please confirm your account by clicking <a href=\"" + callbackUrl + "\">here</a>");

                    return RedirectToAction("UsersList", "Account");
                }
                AddErrors(result);
            }

            // If we got this far, something failed, redisplay form
            return View(model);
        }

        //
        // GET: /Account/UsersList
        [HttpGet]
        [Authorize(Roles = ServerConfigurator.adminRole)]
        public ActionResult UsersList()
        {
            var db = new ApplicationDbContext();
            var list = db.Users.ToList();

            UsersViewModel usersViewModel = new UsersViewModel();
            usersViewModel.AllUsers = new List<ApplicationUser>();

            foreach (ApplicationUser ap in list)
            {
                if (!UserManager.IsInRole(ap.Id, ServerConfigurator.adminRole))
                    usersViewModel.AllUsers.Add(ap);
            }

            return View(usersViewModel);
        }

        //
        // GET: /Account/RemoveUser
        [HttpGet]
        [Authorize(Roles = ServerConfigurator.adminRole)]
        public ActionResult RemoveUser(string userId)
        {
            ApplicationUser applicationUser = UserManager.FindById(userId);

            // If user has been found, then we do some stuff
            if (applicationUser != null)
            {
                return View(new UserRemovalData { EmailConfirmation = applicationUser.Email, UserId = applicationUser.Id });
            }
            else
            {
                // Else return to the default users list
                return RedirectToAction("UsersList");
            }
        }

        //
        // POST: /Account/RemoveUser
        [HttpPost]
        [ValidateAntiForgeryToken]
        [Authorize(Roles = ServerConfigurator.adminRole)]
        public ActionResult RemoveUser(UserRemovalData userRemovalData)
        {
            if (ModelState.IsValid)
            {
                if (userRemovalData.UserId != null) // check if in request is an ID
                {
                    ApplicationUser user = UserManager.FindById(userRemovalData.UserId); // look for a user in database

                    if (user != null) // if user was found
                    {
                        if (userRemovalData.EmailForRemoval.Equals(user.Email)) // if an email supplied from user (Administrator) requesting deletion is the same with the email found in the database, then remove
                        {
                            if (!UserManager.IsInRole(user.Id, ServerConfigurator.adminRole)) // cannot delete Admin user
                                UserManager.Delete(user); // remove an user from database

                            return RedirectToAction("UsersList");
                        }
                        else
                        {
                            ViewBag.WrongEmailAddress = "Podany adres email jest niezgodny ze skojarzonym dla tego konta";
                        }
                    }
                }
            }
            // If data typed into form is incorrect, then redisplay the form
            return View(userRemovalData);
        }

        //
        // GET: /Account/EditUserAdmin
        [HttpGet]
        [Authorize(Roles = ServerConfigurator.adminRole)]
        public ActionResult EditUserAdmin(String userId)
        {
            ApplicationUser user = UserManager.FindById(userId);

            //bool testPass = UserManager.CheckPassword(user, "pass");

            // If user has been found, then we do some stuff
            if (user != null)
            {
                return View(new UserEdit(user.Id, user.Firstname, user.Lastname, user.Email));
            }
            else
            {
                // Else return to the default users list
                return RedirectToAction("UsersList");
            }
        }

        //
        // GET: /Account/ResetUserPassword
        [HttpGet]
        [Authorize(Roles = ServerConfigurator.adminRole)]
        public ActionResult ResetUserPassword(string userId)
        {
            if (userId == null)
                return View("UsersList");

            ApplicationUser applicationUser = UserManager.FindById(userId);

            if (applicationUser != null)
            {
                return View(new UserPasswordReset { userId = applicationUser.Id });
            }

            return View("UsersList");
        }

        //
        // GET: /Account/ResetUserPassword
        [HttpPost]
        [Authorize(Roles = ServerConfigurator.adminRole)]
        public ActionResult ResetUserPassword(UserPasswordReset userPasswordReset)
        {
            if (ModelState.IsValid)
            {
                if (UserManager.FindById(userPasswordReset.userId) != null)
                    UserManager.ResetPassword(userPasswordReset.userId, UserManager.GeneratePasswordResetToken(userPasswordReset.userId), userPasswordReset.Password); // reset password

                return RedirectToAction("UsersList");
            }

            // If provided data from client is invalid
            return View(userPasswordReset);
        }

        //
        // POST: /Account/EditUserAdmin
        [HttpPost]
        [ValidateAntiForgeryToken]
        [Authorize(Roles = ServerConfigurator.adminRole)]
        public ActionResult EditUserAdmin(UserEdit userEdit)
        {
            if (ModelState.IsValid)
            {
                if (userEdit.userId == null)
                    return RedirectToAction("UsersList");
                try
                {
                    ApplicationUser applicationUser = UserManager.FindById(userEdit.userId); // find user for editing in database
                                                                                             // ec33c601-3f79-40bd-981d-5604cc3a770c - testowe
                    applicationUser.Firstname = userEdit.FirstName; // set new name
                    applicationUser.Lastname = userEdit.LastName; // set new surname
                    applicationUser.Email = userEdit.Email; // set new email                    

                    UserManager.Update(applicationUser); // Update changes on database

                    return RedirectToAction("UsersList"); // back to the users list
                }
                catch (Exception)
                {
                    return RedirectToAction("UsersList");
                }
            }

            return View(userEdit);
        }

        //
        // GET: /Account/MatlabScriptsList
        [Authorize(Roles = ServerConfigurator.adminRole)]
        public ActionResult MatlabScriptsList()
        {
            string[] files = Directory.GetFiles(Server.MapPath(ServerConfigurator.matlabScriptsPath)); // Read all matlab algorithms available on server

            return View(new MatlabScriptsViewModel { MatlabScripts = files });
        }

        //
        // POST: /Account/MatlabScriptsList
        [HttpPost]
        [Authorize(Roles = ServerConfigurator.adminRole)]
        public ActionResult MatlabScriptsList(HttpPostedFileBase httpPostedFileBase, MatlabScriptsViewModel matlabScriptsViewModel)
        {
            if (httpPostedFileBase != null &&  httpPostedFileBase.ContentLength > 0)
            {                
                if (!Path.GetExtension(httpPostedFileBase.FileName).Equals(".m")) // if there is other than matlab file extension uploaded - then do nothing
                {
                    return RedirectToAction("MatlabScriptsList");
                }

                string path = Path.Combine(Server.MapPath(ServerConfigurator.matlabScriptsPath), httpPostedFileBase.FileName);
                try
                {
                    httpPostedFileBase.SaveAs(path);
                }
                catch (Exception) { }                
            }
            return RedirectToAction("MatlabScriptsList");
        }

        //
        // GET: /Account/RemoveScript
        [HttpGet]
        [Authorize(Roles = ServerConfigurator.adminRole)]
        public ActionResult RemoveMatlabScript(string scriptName)
        {            
            return View(new MatlabViewModel { ScriptName = scriptName});
        }

        //
        // POST: /Account/RemoveScript
        [HttpPost]
        [Authorize(Roles = ServerConfigurator.adminRole)]
        [ValidateAntiForgeryToken]
        public ActionResult RemoveMatlabScript(MatlabViewModel matlabViewModel)
        {
            if (ModelState.IsValid)
            {
                string path = Path.Combine(Server.MapPath(ServerConfigurator.matlabScriptsPath), matlabViewModel.ScriptName);
                if (matlabViewModel.ScriptName.Equals(matlabViewModel.ScriptNameForRemoval))
                {
                    if (System.IO.File.Exists(path))
                    {
                        System.IO.File.Delete(path);
                    }
                }
                else
                {
                    ViewBag.WrongScriptName = "Proszę podać poprawną nazwę skryptu do usunięcia.";
                    return View(matlabViewModel);
                }
                return RedirectToAction("MatlabScriptsList");
            }
            return View(matlabViewModel);            
        }

        //
        // GET: /Account/ConfirmEmail
        [AllowAnonymous]
        public async Task<ActionResult> ConfirmEmail(string userId, string code)
        {
            if (userId == null || code == null)
            {
                return View("Error");
            }
            var result = await UserManager.ConfirmEmailAsync(userId, code);
            return View(result.Succeeded ? "ConfirmEmail" : "Error");
        }

        //
        // GET: /Account/ForgotPassword
        [AllowAnonymous]
        public ActionResult ForgotPassword()
        {
            return View();
        }

        //
        // POST: /Account/ForgotPassword
        [HttpPost]
        [AllowAnonymous]
        [ValidateAntiForgeryToken]
        public async Task<ActionResult> ForgotPassword(ForgotPasswordViewModel model)
        {
            if (ModelState.IsValid)
            {
                var user = await UserManager.FindByNameAsync(model.Email);
                if (user == null || !(await UserManager.IsEmailConfirmedAsync(user.Id)))
                {
                    // Don't reveal that the user does not exist or is not confirmed
                    return View("ForgotPasswordConfirmation");
                }

                // For more information on how to enable account confirmation and password reset please visit https://go.microsoft.com/fwlink/?LinkID=320771
                // Send an email with this link
                // string code = await UserManager.GeneratePasswordResetTokenAsync(user.Id);
                // var callbackUrl = Url.Action("ResetPassword", "Account", new { userId = user.Id, code = code }, protocol: Request.Url.Scheme);		
                // await UserManager.SendEmailAsync(user.Id, "Reset Password", "Please reset your password by clicking <a href=\"" + callbackUrl + "\">here</a>");
                // return RedirectToAction("ForgotPasswordConfirmation", "Account");
            }

            // If we got this far, something failed, redisplay form
            return View(model);
        }

        //
        // GET: /Account/ForgotPasswordConfirmation
        [AllowAnonymous]
        public ActionResult ForgotPasswordConfirmation()
        {
            return View();
        }

        //
        // GET: /Account/ResetPassword
        [AllowAnonymous]
        public ActionResult ResetPassword(string code)
        {
            return code == null ? View("Error") : View();
        }

        //
        // POST: /Account/ResetPassword
        [HttpPost]
        [AllowAnonymous]
        [ValidateAntiForgeryToken]
        public async Task<ActionResult> ResetPassword(ResetPasswordViewModel model)
        {
            if (!ModelState.IsValid)
            {
                return View(model);
            }
            var user = await UserManager.FindByNameAsync(model.Email);
            if (user == null)
            {
                // Don't reveal that the user does not exist
                return RedirectToAction("ResetPasswordConfirmation", "Account");
            }
            var result = await UserManager.ResetPasswordAsync(user.Id, model.Code, model.Password);
            if (result.Succeeded)
            {
                return RedirectToAction("ResetPasswordConfirmation", "Account");
            }
            AddErrors(result);
            return View();
        }

        //
        // GET: /Account/ResetPasswordConfirmation
        [AllowAnonymous]
        public ActionResult ResetPasswordConfirmation()
        {
            return View();
        }

        //
        // POST: /Account/ExternalLogin
        [HttpPost]
        [AllowAnonymous]
        [ValidateAntiForgeryToken]
        public ActionResult ExternalLogin(string provider, string returnUrl)
        {
            // Request a redirect to the external login provider
            return new ChallengeResult(provider, Url.Action("ExternalLoginCallback", "Account", new { ReturnUrl = returnUrl }));
        }

        //
        // GET: /Account/SendCode
        [AllowAnonymous]
        public async Task<ActionResult> SendCode(string returnUrl, bool rememberMe)
        {
            var userId = await SignInManager.GetVerifiedUserIdAsync();
            if (userId == null)
            {
                return View("Error");
            }
            var userFactors = await UserManager.GetValidTwoFactorProvidersAsync(userId);
            var factorOptions = userFactors.Select(purpose => new SelectListItem { Text = purpose, Value = purpose }).ToList();
            return View(new SendCodeViewModel { Providers = factorOptions, ReturnUrl = returnUrl, RememberMe = rememberMe });
        }

        //
        // POST: /Account/SendCode
        [HttpPost]
        [AllowAnonymous]
        [ValidateAntiForgeryToken]
        public async Task<ActionResult> SendCode(SendCodeViewModel model)
        {
            if (!ModelState.IsValid)
            {
                return View();
            }

            // Generate the token and send it
            if (!await SignInManager.SendTwoFactorCodeAsync(model.SelectedProvider))
            {
                return View("Error");
            }
            return RedirectToAction("VerifyCode", new { Provider = model.SelectedProvider, ReturnUrl = model.ReturnUrl, RememberMe = model.RememberMe });
        }

        //
        // GET: /Account/ExternalLoginCallback
        [AllowAnonymous]
        public async Task<ActionResult> ExternalLoginCallback(string returnUrl)
        {
            var loginInfo = await AuthenticationManager.GetExternalLoginInfoAsync();
            if (loginInfo == null)
            {
                return RedirectToAction("Login");
            }

            // Sign in the user with this external login provider if the user already has a login
            var result = await SignInManager.ExternalSignInAsync(loginInfo, isPersistent: false);
            switch (result)
            {
                case SignInStatus.Success:
                    return RedirectToLocal(returnUrl);
                case SignInStatus.LockedOut:
                    return View("Lockout");
                case SignInStatus.RequiresVerification:
                    return RedirectToAction("SendCode", new { ReturnUrl = returnUrl, RememberMe = false });
                case SignInStatus.Failure:
                default:
                    // If the user does not have an account, then prompt the user to create an account
                    ViewBag.ReturnUrl = returnUrl;
                    ViewBag.LoginProvider = loginInfo.Login.LoginProvider;
                    return View("ExternalLoginConfirmation", new ExternalLoginConfirmationViewModel { Email = loginInfo.Email });
            }
        }

        //
        // POST: /Account/ExternalLoginConfirmation
        [HttpPost]
        [AllowAnonymous]
        [ValidateAntiForgeryToken]
        public async Task<ActionResult> ExternalLoginConfirmation(ExternalLoginConfirmationViewModel model, string returnUrl)
        {
            if (User.Identity.IsAuthenticated)
            {
                return RedirectToAction("Index", "Manage");
            }

            if (ModelState.IsValid)
            {
                // Get the information about the user from the external login provider
                var info = await AuthenticationManager.GetExternalLoginInfoAsync();
                if (info == null)
                {
                    return View("ExternalLoginFailure");
                }
                var user = new ApplicationUser { UserName = model.Email, Email = model.Email };
                var result = await UserManager.CreateAsync(user);
                if (result.Succeeded)
                {
                    result = await UserManager.AddLoginAsync(user.Id, info.Login);
                    if (result.Succeeded)
                    {
                        await SignInManager.SignInAsync(user, isPersistent: false, rememberBrowser: false);
                        return RedirectToLocal(returnUrl);
                    }
                }
                AddErrors(result);
            }

            ViewBag.ReturnUrl = returnUrl;
            return View(model);
        }

        //
        // POST: /Account/LogOff
        [HttpPost]
        [ValidateAntiForgeryToken]
        public ActionResult LogOff()
        {
            AuthenticationManager.SignOut(DefaultAuthenticationTypes.ApplicationCookie);
            return RedirectToAction("Index", "Home");
        }

        //
        // POST: /Account/MobileLogOff
        [HttpPost]
        public HttpStatusCodeResult MobileLogOff()
        {
            AuthenticationManager.SignOut(DefaultAuthenticationTypes.ApplicationCookie);
            return new HttpStatusCodeResult(HttpStatusCode.OK);
        }

        //
        // GET: /Account/ExternalLoginFailure
        [AllowAnonymous]
        public ActionResult ExternalLoginFailure()
        {
            return View();
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                if (_userManager != null)
                {
                    _userManager.Dispose();
                    _userManager = null;
                }

                if (_signInManager != null)
                {
                    _signInManager.Dispose();
                    _signInManager = null;
                }
            }

            base.Dispose(disposing);
        }

        #region Helpers
        // Used for XSRF protection when adding external logins
        private const string XsrfKey = "XsrfId";

        private IAuthenticationManager AuthenticationManager
        {
            get
            {
                return HttpContext.GetOwinContext().Authentication;
            }
        }

        private void AddErrors(IdentityResult result)
        {
            foreach (var error in result.Errors)
            {
                ModelState.AddModelError("", error);
            }
        }

        private ActionResult RedirectToLocal(string returnUrl)
        {
            if (Url.IsLocalUrl(returnUrl))
            {
                return Redirect(returnUrl);
            }
            return RedirectToAction("Index", "Home");
        }

        internal class ChallengeResult : HttpUnauthorizedResult
        {
            public ChallengeResult(string provider, string redirectUri)
                : this(provider, redirectUri, null)
            {
            }

            public ChallengeResult(string provider, string redirectUri, string userId)
            {
                LoginProvider = provider;
                RedirectUri = redirectUri;
                UserId = userId;
            }

            public string LoginProvider { get; set; }
            public string RedirectUri { get; set; }
            public string UserId { get; set; }

            public override void ExecuteResult(ControllerContext context)
            {
                var properties = new AuthenticationProperties { RedirectUri = RedirectUri };
                if (UserId != null)
                {
                    properties.Dictionary[XsrfKey] = UserId;
                }
                context.HttpContext.GetOwinContext().Authentication.Challenge(properties, LoginProvider);
            }
        }
        #endregion
    }


}