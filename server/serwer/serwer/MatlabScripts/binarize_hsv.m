function [I] = binarize_hsv(imageSource, imageDestination, afterProcessingData)
    I = imread(imageSource);
    
    if ~isinteger(I)
        I = im2uint8(I);
    end
    I = rgb2hsv(I);

    for i=1:size(I,1)
        for j=1:size(I,2)
            if (I(i,j,1) > 0.7 && I(i,j,1) < 0.9) && I(i,j,2) > 0.085
                I(i,j,1) = 0;
                I(i,j,2) = 0;
                I(i,j,3) = 1;
            else
                I(i,j,1) = 0;
                I(i,j,2) = 0;
                I(i,j,3) = 0;
            end
         end
    end
    I = hsv2rgb(I);
    imwrite(I,imageDestination);
    
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
