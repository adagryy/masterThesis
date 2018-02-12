using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Linq;
using System.Web;

namespace serwer.Models
{

    public abstract class UserData
    {
        [Required]
        [EmailAddress]
        public string Email { get; set; }

        //[Required]
        //[DataType(DataType.Password)]
        //protected string Password { get; set; }

        public string Token { get; set; }

    }
    public class LoginMobileViewModel
    {
        [Required]
        [EmailAddress]
        public string Email { get; set; }

        [Required]
        [DataType(DataType.Password)]
        public string Password { get; set; }
    }

    public class LoginCheck : UserData
    {

    }

    public class HandleImage : UserData
    {
        [Required]
        public string Image { get; set; }

        [Required]
        public string ImageHash { get; set; }
    }

    //public class LoginMobileResource
    //{
    //    [Required]
    //    [EmailAddress]
    //    public string Email { get; set; }

    //    [Required]
    //    [DataType(DataType.Password)]
    //    public string Password { get; set; }

    //    [Display(Name = "Remember me?")]
    //    public bool RememberMe { get; set; }

    //    public String Status { get; set; }

    //    public String UserId { get; set; }
    //}
}