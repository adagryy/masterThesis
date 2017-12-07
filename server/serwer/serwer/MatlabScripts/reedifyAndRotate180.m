function [ ] = reedifyAndRotate180( imageSource, imageDestination, afterProcessingData )
	% Returns only red oart of image in RGB model and rotates image in 180 degrees
    
    s = struct; % Create struct
    
    % Image processing
    I = im2double(imread(imageSource));
    z = size(I, 3);
    if z == 3
    	I(:,:,2) = 0;
    	I(:,:,3) = 0;
    	I = imrotate(I, 180);
    end
    
    s.totalSurface = 10; % Fill fields of a structure
    s.totalAmount = 3;
    
    imwrite(I, imageDestination); % Save processed image into server disk
    
    % Save json-formatted details obtained during processing into server
    % disk
    text = jsonencode(s); % Encode given struct 's' in json format
    fileId = fopen(afterProcessingData,'wt'); % Create file 
    fprintf(fileId, text); % Save data to a disk
    fclose(fileId); % close file
end
