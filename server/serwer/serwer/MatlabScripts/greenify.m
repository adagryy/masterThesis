function [ ] = imageNegative( imageSource, imageDestination )
    % Returns only Green oart of image in RGB model
    
    I = im2double(imread(imageSource));
    z = size(I, 3);
    if z == 3
    	I(:,:,1) = 0;
    	I(:,:,3) = 0;
	end
    imwrite(I, imageDestination);
end
