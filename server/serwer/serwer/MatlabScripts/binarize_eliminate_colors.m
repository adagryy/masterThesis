function [] = binarize_eliminate_colors(imageSource, imageDestination, afterProcessingData)
    I = imread(imageSource);
    
    if ~isinteger(I)
        I = im2uint8(I);
    end
    
    for i=1:size(I,1)
        for j=1:size(I,2)
            if I(i,j,1) >= 130
                if I(i,j,2) >= 130
                    if I(i,j,3) >= 130
                        I(i,j,:) = 0;
                    end
                end
            end
            if I(i,j,1) >= 80 && I(i,j,1) <= 140
                if I(i,j,2) >= 80 && I(i,j,2) <= 130
                    if I(i,j,3) >= 85 && I(i,j,3) <= 125
                        I(i,j,:) = 0; 
                    end
                end
            end
            if I(i,j,1) >= 130
                if I(i,j,2) >= 130
                    if I(i,j,3) <= 50
                        I(i,j,:) = 0;
                    end
                end
            end
            if I(i,j,1) ~= 0
                I(i,j,:) = 255;
            end
            
        end
    end
    I = poprawa(I, [11 11]);
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

function [I] = poprawa(I, mask)
    if ~isinteger(I)
        I = im2uint8(I);
    end
    % obraz w uint8
    r=medfilt2(I(:,:,1), mask); 
    g=medfilt2(I(:,:,2), mask); 
    b=medfilt2(I(:,:,3), mask); 
    I=cat(3,r,g,b);

    sigma = 0.5;
    alpha = 0.6;

    I = locallapfilt(I, sigma, alpha, 'NumIntensityLevels', 15);
end
