using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;

namespace serwer.Helpers
{
    // This class stores information which will be used for exact image processing purposses by Matlab engine
    public class MatlabProcessingDataThreaded
    {
        private string 
                        imageSource, // it is full path to the folder with source image
                        imageDestination, // it is full path to the folder where image obtained from processing should be saved. It is, by default, the same as "imageSource"
                        afterProcessingFileDestination, // it is full path to the folder where JSON file obtained from processing should be saved 
                        afterProcessingFileName, // it is a filename of file into which Matlab will save data obtained from image processing
                        afterProcessingFileExtension, // extension of "afterProcessingFileName" IT MUST CONTAIN A DOT CHARACTER AT THE BEGINNING ("ServerConfigurator" class)
                        //userInvokingProcessing, // username [NO NEED TO USE HERE]
                        originalFileName, // filename of the file which will be processed
                        processedFileName,  // filename of the file obtained from image processing
                        selectedProcessingAlgorithm, // name of algorithm which will be used for processing purposes
                        matlabScriptsDirectory; // full path to the directory which has matlab scripts used for image processing
        public string ImageSource { get { return imageSource; } }
        public string ImageDestination { get { return imageDestination; } }
        //public string UserInvokingProcessing { get { return userInvokingProcessing; } } // username [NO NEED TO USE HERE]
        public string OriginalFileName { get { return originalFileName; } }
        public string ProcessedFileName { get { return processedFileName; } }
        public string SelectedProcessingAlgorithm { get { return selectedProcessingAlgorithm; } }
        public string MatlabScriptsDirectory { get { return matlabScriptsDirectory; } }
        public string AfterProcessingFileName { get { return afterProcessingFileName; } }
        public string AfterProcessingFileExtension { get { return afterProcessingFileExtension; } }
        public string AfterProcessingFileDestination { get { return afterProcessingFileDestination; } }


        public MatlabProcessingDataThreaded(string imageSource, string imageDestination, string afterProcessingFileDestination, string afterProcessingFileName, string afterProcessingFileExtension,
            /* string userInvokingProcessing, */ string originalFileName, 
            string processedFileName, string selectedProcessingAlgorithm, string matlabScriptsDirectory)
        {
            this.imageSource = imageSource;
            this.imageDestination = imageDestination;
            this.afterProcessingFileDestination = afterProcessingFileDestination;
            this.afterProcessingFileName = afterProcessingFileName;
            this.afterProcessingFileExtension = afterProcessingFileExtension;            
            //this.userInvokingProcessing = userInvokingProcessing; // username [NO NEED TO USE HERE]
            this.originalFileName = originalFileName;
            this.processedFileName = processedFileName;
            this.selectedProcessingAlgorithm = selectedProcessingAlgorithm;
            this.matlabScriptsDirectory = matlabScriptsDirectory;
        }
    }
}