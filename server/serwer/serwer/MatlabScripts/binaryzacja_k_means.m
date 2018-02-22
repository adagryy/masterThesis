function [] = binaryzacja_k_means(imageSource, imageDestination, afterProcessingData)
    I = imread(imageSource);
    
    if isinteger(I)
        I = im2double(I);
    end
    
    k_means(I,3,imageDestination);
    
    s = struct; % Create struct
    s.totalSurface = 10; % Fill fields of a structure
    s.totalAmount = 3;
    
    % Save json-formatted details obtained during processing into server
    % disk
    text = jsonencode(s); % Encode given struct 's' in json format
    fileId = fopen(afterProcessingData,'wt'); % Create file 
    fprintf(fileId, text); % Save data to a disk
    fclose(fileId); % close file
end