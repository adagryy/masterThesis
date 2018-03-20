using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;

namespace serwer.Models
{
    public class UsersViewModel
    {
        public IEnumerable<ApplicationUser> AllUsers { get; set; }
    }

    public class MatlabScriptsViewModel
    {
        public IEnumerable<String> MatlabScripts { get; set; }
    }
}