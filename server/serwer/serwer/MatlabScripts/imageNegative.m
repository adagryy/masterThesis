function [ negativeOfImage ] = imageNegative( imageSource, imageDestination )
    % Calculates and returns negative of a given image
    
    I = imread(imageSource);
    negativeOfImage = imcomplement(im2double(I)); % calculates here a negative of given image
    imwrite(negativeOfImage, imageDestination);
end

