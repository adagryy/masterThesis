function [ ] = imageNegative( imageSource, imageDestination )
	% Returns only red oart of image in RGB model and rotates image in 180 degrees
    
    I = im2double(imread(imageSource));
    z = size(I, 3);
    if z == 3
    	I(:,:,2) = 0;
    	I(:,:,3) = 0;
    	I = imrotate(I, 180);
	end
    imwrite(I, imageDestination);
end
