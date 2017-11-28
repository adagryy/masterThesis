function [ ] = imageNegative( imageSource, imageDestination )
    % Calculates and returns negative of a given image
    
    I = im2double(imread(imageSource));
    I = imcomplement(I); % calculates here a negative of given image
    imwrite(I, imageDestination);
end

