using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;

namespace serwer.Config
{
    public static class ServerConfigurator
    {
        // This class contains configuration parameters for whole application
        public const string originalImageName = "original"; // name for image, which is uploaded into serwer. The extension of image is the same as extension of uploaded file
        public const string processedImageName = "processed"; // name of image which is saved after processing. Extension is inherited from an original image
        public const string imageStoragePath = "~/Storage/"; // VERY VERY important is a slash character ("/") at the end of this string. Tilde means relative path to the directory. 
                                                             // Slash is required when building path to the image
                                                             // [...] It it the path, where there are personal folders with original and processed images
        public const string matlabScriptsPath = "~/MatlabScripts/"; // As above: VERY VERY important is a slash character ("/") at the end of this string. Tilde means relative path to the directory. 
                                                                    // It is a path to the folder which stores choosable by user matlab scripts used for image processing.

    }
}