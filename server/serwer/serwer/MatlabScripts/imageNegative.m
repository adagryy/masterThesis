function [ negativeOfImage ] = imageNegative( imageSource, imageDestination, afterProcessingData )
    % Calculates and returns negative of a given image

    s = struct; % Create struct

    % Image processing
    I = imread(imageSource);
    I = imcomplement(im2double(I)); % calculates here a negative of given image

    s.totalSurface = 10; % Fill fields of a structure
    s.totalAmount = "3asd435";

    imwrite(I, imageDestination); % Save processed image into server disk

    % Save json-formatted details obtained during processing into server
    % disk
    text = jsonencode(s); % Encode given struct 's' in json format
    fileId = fopen(afterProcessingData,'wt'); % Create file
    fprintf(fileId, text); % Save data to a disk
    fclose(fileId); % close file
end
