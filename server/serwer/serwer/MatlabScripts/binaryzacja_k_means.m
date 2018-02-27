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

function [] = k_means(I,nColors, imageDestination)
    % przechodzimy na przestrzeñ kolorów L*a*b
    cform = makecform('srgb2lab');
    lab_image = applycform(I,cform);

    %figure(),imshow(lab_image);
    
    % wybieramy przestrzeñ a*b* gdzie jest nasz obraz
    ab = double(lab_image(:,:,2:3));
    
    nrows = size(ab,1);
    ncols = size(ab,2);
    ab = reshape(ab,nrows*ncols,2);
    
    [cluster_idx, ~] = kmeans(ab,nColors,'distance','sqeuclidean', 'Replicates', 2);
    
    pixel_labels = reshape(cluster_idx,nrows,ncols);
    
    segmented_images = cell(1,nColors);
    rgb_label = repmat(pixel_labels,[1 1 3]);
    
    for k=1:nColors
        color = I;
        color(rgb_label ~= k) = 0;
        segmented_images{k} = color;
        %figure(), imshow(segmented_images{k});
    end
    
    max_k = 0;
    for k=1:nColors
        rgb_columns = reshape(segmented_images{k}, [], 3);
        [~, ~, n] = unique(rgb_columns, 'rows');
        color_counts = accumarray(n, 1);
        
        %fprintf('There are %d unique colors in the image.\n', size(unique_colors, 1));
        [max_count, ~] = max(color_counts);
        
        %fprintf('The color [%d %d %d] occurs %d times.\n', ...
        %    unique_colors(idx, 1), unique_colors(idx, 2), ...
        %    unique_colors(idx, 3), max_count);
        if max_count / (size(I,1)*size(I,2)) > 0.99
            max_k = k;
            break;
        end
    end
    
    if max_k == 0
        k_means(I,nColors+1,imageDestination);
        return
    else
        K = imbinarize(rgb2gray(segmented_images{max_k}),'adaptiv','ForegroundPolarity','dark','Sensitivity',0.45);
        imwrite(K,imageDestination);
    end
end