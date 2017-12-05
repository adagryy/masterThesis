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

    public class ImagesDownloadDetails
    {
        public List<string> items { get; set; }
        public string originalImageExtension { get; set; }
        public string processedImageExtension { get; set; }
    }
}