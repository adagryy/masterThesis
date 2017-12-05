function [ ] = imageNegative( imageSource, imageDestination )
    % Returns only Green oart of image in RGB model
    
    I = im2double(imread(imageSource));
    I = imgaussfilt(I, 2);
    I = imrotate(I, 180);
    imwrite(I, imageDestination);
end
