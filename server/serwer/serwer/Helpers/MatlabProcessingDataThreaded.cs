using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;

namespace serwer.Helpers
{
    public class MatlabProcessingDataThreaded
    {
        private string imageSource, imageDestination, userInvokingProcessing, originalFileName, processedFileName, selectedProcessingAlgorithm, matlabScriptsDirectory;
        public string ImageSource { get { return imageSource; } }
        public string ImageDestination { get { return imageDestination; } }
        //public string UserInvokingProcessing { get { return userInvokingProcessing; } }
        public string OriginalFileName { get { return originalFileName; } }
        public string ProcessedFileName { get { return processedFileName; } }
        public string SelectedProcessingAlgorithm { get { return selectedProcessingAlgorithm; } }
        public string MatlabScriptsDirectory { get { return matlabScriptsDirectory; } }


        public MatlabProcessingDataThreaded(string imageSource, string imageDestination, /* string userInvokingProcessing, */ string originalFileName, 
            string processedFileName, string selectedProcessingAlgorithm, string matlabScriptsDirectory)
        {
            this.imageSource = imageSource;
            this.imageDestination = imageDestination;
            //this.userInvokingProcessing = userInvokingProcessing;
            this.originalFileName = originalFileName;
            this.processedFileName = processedFileName;
            this.selectedProcessingAlgorithm = selectedProcessingAlgorithm;
            this.matlabScriptsDirectory = matlabScriptsDirectory;
        }
    }
}