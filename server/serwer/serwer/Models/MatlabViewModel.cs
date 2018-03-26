using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Linq;
using System.Web;

namespace serwer.Models
{
    public class MatlabViewModel
    {
        [Required]
        public string ScriptName { get; set; }

        [Required(ErrorMessage = "Proszę wpisać nazwę skryptu")]
        [Display(Name = "Wpisz nazwę skryptu do usunięcia")]
        public string ScriptNameForRemoval { get; set; }
    }
}