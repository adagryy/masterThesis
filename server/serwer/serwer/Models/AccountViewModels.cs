using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;

namespace serwer.Models
{
    public class ExternalLoginConfirmationViewModel
    {
        [Required]
        [Display(Name = "Email")]
        public string Email { get; set; }
    }

    public class ExternalLoginListViewModel
    {
        public string ReturnUrl { get; set; }
    }

    public class SendCodeViewModel
    {
        public string SelectedProvider { get; set; }
        public ICollection<System.Web.Mvc.SelectListItem> Providers { get; set; }
        public string ReturnUrl { get; set; }
        public bool RememberMe { get; set; }
    }

    public class VerifyCodeViewModel
    {
        [Required]
        public string Provider { get; set; }

        [Required]
        [Display(Name = "Kod")]
        public string Code { get; set; }
        public string ReturnUrl { get; set; }

        [Display(Name = "Zapamiętaj mnie")]
        public bool RememberBrowser { get; set; }

        public bool RememberMe { get; set; }
    }

    public class ForgotViewModel
    {
        [Required]
        [Display(Name = "Adres Email")]
        public string Email { get; set; }
    }

    public class LoginViewModel
    {
        [Required]
        [Display(Name = "Adres Email")]
        [EmailAddress]
        public string Email { get; set; }

        [Required]
        [DataType(DataType.Password)]
        [Display(Name = "Hasło")]
        public string Password { get; set; }

        [Display(Name = "Zapamiętaj mnie")]
        public bool RememberMe { get; set; }
    }

    public class RegisterViewModel
    {
        [Required(ErrorMessage = "Proszę wpisać imię")]
        [Display(Name = "Imię")]
        public string Firstname { get; set; }

        [Required(ErrorMessage = "Proszę wpisać nazwisko")]
        [Display(Name = "Nazwisko")]
        public string Lastname { get; set; }

        [Required(ErrorMessage = "Proszę podać nazwę użytkownika")]
        [Display(Name = "Nazwa użytkownika")]
        public string UserName { get; set; }

        [Required(ErrorMessage = "Proszę podać adres email")]
        [EmailAddress(ErrorMessage = "Adres email ma niepoprawny format")]
        [Display(Name = "Adres Email")]
        public string Email { get; set; }

        [Required(ErrorMessage = "Proszę podać hasło")]
        [StringLength(100,  MinimumLength = 6, ErrorMessage = "Długość pola {0} musi być z przedziału {1} - {2}")] // {0} - field name (current value: "Hasło"), {1} - first argument (current value: 100), {2} - second argument (current value: 6)
        [DataType(DataType.Password)]
        [Display(Name = "Hasło")]
        public string Password { get; set; }

        [DataType(DataType.Password)]
        [Display(Name = "Potwierdzenie hasła")]
        [Compare("Password", ErrorMessage = "Wprowadzone hasła nie są identyczne")]
        public string ConfirmPassword { get; set; }
    }

    public class ResetPasswordViewModel
    {
        [Required]
        [EmailAddress]
        [Display(Name = "Adres Email")]
        public string Email { get; set; }

        [Required]
        [StringLength(100, ErrorMessage = "Długość pola {0} musi być z przedziału {1} - {2}", MinimumLength = 6)]
        [DataType(DataType.Password)]
        [Display(Name = "Password")]
        public string Password { get; set; }

        [DataType(DataType.Password)]
        [Display(Name = "Confirm password")]
        [Compare("Password", ErrorMessage = "Wprowadzone hasła nie są identyczne")]
        public string ConfirmPassword { get; set; }

        public string Code { get; set; }
    }

    public class ForgotPasswordViewModel
    {
        [Required]
        [EmailAddress]
        [Display(Name = "Adres Email")]
        public string Email { get; set; }
    }
}
