using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using System.Web.Mvc;

namespace serwer.Models
{
    public class UploadFileViewModel
    {
        public SelectList algorithms { get; set; }
        public string selectedAlgorithm { get; set; }
    }
}