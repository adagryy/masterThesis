function [ ] = blurr( imageSource, imageDestination, afterProcessingData )
    % Returns only Green oart of image in RGB model
    
    s = struct; % Create struct
    
    % Image processing
    I = im2double(imread(imageSource));
    I = imgaussfilt(I, 10);
    I = imrotate(I, 180);
    
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
