using System.IO;

namespace serwer.Config
{
    public static class ServerConfigurator
    {
        // This class contains configuration parameters for whole application
        public const string originalImageName = "original"; // name for image, which is uploaded into serwer. The extension of image is the same as extension of uploaded file
        public const string processedImageName = "processed"; // name of image which is saved after processing. Extension is inherited from an original image
        //public const string imageStoragePath = "~/Storage/"; // VERY VERY important is a slash character ("/") at the end of this string. Tilde means relative path to the directory. 
                                                             // Slash is required when building path to the image
                                                             // [...] It it the path, where there are personal folders with original and processed images
        //public const string matlabScriptsPath = "~/MatlabScripts/"; // As above: VERY VERY important is a slash character ("/") at the end of this string. Tilde means relative path to the directory. 
                                                                    // It is a path to the folder which stores choosable by user matlab scripts used for image processing.
        public const string afterProcessingDataFileName = "processingResults"; // filename of a file into which matlab scripts will save informations about detected objects on image.
        public const string afterProcessingDataFileExtension = ".json"; // VERY VERY important is a dot on the beginning of this string. It is an extension (default: json) into which matlab scripts will save informations about detected objects on image.

        public const string adminRole = "Administrator"; // Name of admin role

        public const string usersStorage = "C:\\Users\\grycz\\Desktop\\Storage\\users\\"; // storage for images (all users have their own directory) out of the IIS server
        public const string matlabScripts = "C:\\Users\\grycz\\Desktop\\Storage\\matlabscripts\\"; // storage for matlab scripts; out of the IIS server
        public const string directoryPathSeparator = "\\"; // separates folders in path string. For example in physical path it is double backslash (for example "C:\\Example") and for virtual path it is single slash (for example "~/Example/")        

        public const string supportedImageExtensions = "(jpg|jpeg|png|gif|bmp)"; // image extensions supported by server in procesing. This is part of the regular expression

        public static void initializeStorageDirectories()
        {
            if (!Directory.Exists(usersStorage)) // if not exists usersStorage directory, then create it
            {
                Directory.CreateDirectory(usersStorage);
            }

            if (!Directory.Exists(matlabScripts)) // if not exists matlabScripts directory, then create it
            {
                Directory.CreateDirectory(matlabScripts);
            }
        }
    }
}