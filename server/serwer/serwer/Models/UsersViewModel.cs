using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Linq;
using System.Web;

namespace serwer.Models
{
    public class UsersViewModel
    {
        [Required]
        public List<ApplicationUser> AllUsers { get; set; }
    }

    public class MatlabScriptsViewModel
    {
        [Required]
        public IEnumerable<String> MatlabScripts { get; set; }
    }

    public class UserEdit
    {
        public UserEdit() { }

        public UserEdit(string userId, string FirstName, string LastName, string Email)
        {
            this.userId = userId;
            this.FirstName = FirstName;
            this.LastName = LastName;
            this.Email = Email;
        }

        [Required(ErrorMessage = "UserId field is required on the serwer app")]
        public string userId { get; set; }

        [Required(ErrorMessage ="Proszę podać imię użytkownika")]
        [Display(Name = "Imię")]
        public String FirstName { get; set; }

        [Required(ErrorMessage = "Proszę podać nazwisko użytkownika")]
        [Display(Name = "Nazwisko")]
        public String LastName { get; set; }

        [Required(ErrorMessage = "Proszę podać adres email")]
        [Display(Name = "Adres Email")]
        [EmailAddress(ErrorMessage = "Podany adres email ma niewłaściwy format")]
        public String Email { get; set; }
    }

    public class UserRemovalData
    {
        [Required]
        public string EmailConfirmation { get; set; }

        [Required]
        public string UserId { get; set; }

        [Required(ErrorMessage = "Proszę podać adres email")]
        [Display(Name = "Potwierdź adres email")]
        [EmailAddress(ErrorMessage = "Adres email posiada niepoprawny format")]
        public string EmailForRemoval { get; set; }
    }

    public class UserPasswordReset
    {
        [Required(ErrorMessage = "UserId field is required on the serwer app")]
        public string userId { get; set; }

        [Required(ErrorMessage = "Proszę podać hasło")]
        [StringLength(100, MinimumLength = 6, ErrorMessage = "Długość pola {0} musi być z przedziału {1} - {2}")] // {0} - field name (current value: "Hasło"), {1} - first argument (current value: 100), {2} - second argument (current value: 6)
        [DataType(DataType.Password)]
        [Display(Name = "Nowe hasło")]
        public string Password { get; set; }

        [DataType(DataType.Password)]
        [Display(Name = "Potwierdź nowe hasło")]
        [Compare("Password", ErrorMessage = "Wprowadzone hasła nie są identyczne")]
        public string ConfirmPassword { get; set; }
    }
}