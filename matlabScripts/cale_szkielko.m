function [] = cale_szkielko(imageSource, imageDestination, afterProcessingData)
    I = imread(imageSource);

    % binarization
    I = binarize_hsv(I);
    I = medfilt2(I, [11 11]);
    
    % skeletonization
    J = bwskel(logical(I));
    %J = bwmorph(I,'skel',Inf);
    
    % segmentation
    [table_1, table_2] = segmentation(J);

    % calculation of length
    table = length_calculation(I, table_1, table_2);
    
    % save image
    colors = save_image(I, table_1, table_2, table, imageDestination);
    results = zeros(size(table,1),5);
    results(:,1:2) = table(:,1:2);
    results(:,3:5) = colors(:,2:4);
    results
%     % save data to JSON
    s = struct;
    s.iloscPaleczek = size(results,1);
    s.dlugosciPaleczek = results;

    % Save json-formatted details obtained during processing into server
    % disk
    text = jsonencode(s); % Encode given struct 's' in json format
    fileId = fopen(afterProcessingData,'wt'); % Create file
    fprintf(fileId, text); % Save data to a disk
    fclose(fileId); % close file
end

function [J] = binarize_hsv(I)
    J = zeros(size(I,1), size(I,2));
    
    if ~isa(I, 'uint8') 
        I = im2uint8(I);
    end
    I = rgb2hsv(I);
    
    for i=1:size(I,1)
        for j=1:size(I,2)
            if (I(i,j,1) > 0.7 && I(i,j,1) < 0.9) && I(i,j,2) > 0.085
                J(i,j) = 1;
            else
                J(i,j) = 0;
            end
         end
    end
    
    J = medfilt2(J, [11 11]);
end

function [table_1, table_3] = segmentation(I)

    if ~isa(I, 'double') 
        I = im2double(I);
    end
    
    table_1 = [];
    image_1 = zeros(size(I,1), size(I,2));

    % first segmentation
    cc_1 = bwconncomp(I, 8);
    for i=1:cc_1.NumObjects
        tmp = cc_1.PixelIdxList{i};
        tmp(:,2) = i;
        table_1 = cat(1,table_1,tmp);
        grain = false(size(I));
        grain(cc_1.PixelIdxList{i}) = true;
        image_1 = image_1 + grain;
    end

    % connecting elements
    bw2 = bwdist(image_1) <= 70;

    % second segmentation
    table_2 = [];
    image_2 = zeros(size(I,1), size(I,2));
    cc_2 = bwconncomp(bw2,8);
    for i=1:cc_2.NumObjects
        tmp = cc_2.PixelIdxList{i};
        tmp(:,2) = i;
        table_2 = cat(1,table_2,tmp);
        grain = false(size(I));
        grain(cc_2.PixelIdxList{i}) = true;
        image_2 = image_2 + grain; 
    end
    
    % differences are correct segmentation
    [~, indexes] = setdiff(table_2,table_1);
    table_3 = table_2;
    table_3(indexes,:) = [];
    
    % saving results to image, first layer is image, second layer is
    % segment
%     [row, col] = ind2sub([size(I,1) size(I,2)], table_3(:,1));
%     indexes = [];
%     indexes = cat(2,indexes,[row col]);
%     J = zeros(size(I,1), size(I,2));
%     J(:,:,2) = 0;
%     for i=1:size(indexes,1)
%         J(indexes(i,1), indexes(i,2),1) = 1;
%         J(indexes(i,1), indexes(i,2),2) = table_3(i,2);
%     end
end

function [final_table] = length_calculation(I, table_1, table_2) 
    tabela = zeros(size(unique(table_1(:,2)),1),3);
    tabela(1:end,1) = 1:size(tabela,1);
    for i=1:size(table_1,1)
        tabela(table_1(i,2), 2) = tabela(table_1(i,2), 2) + 1;
    
        if tabela(table_1(i,2), 3) == 0
            [row, ~] = find(table_2(:,1) == table_1(i,1));
            tabela(table_1(i,2), 3) = table_2(row,2);
        end
    end

    indexes = [];
    for i=1:size(tabela,1)
        if tabela(i,2) < 15
            indexes = cat(1,indexes,i);
        end
    end

    tabela(indexes,:) = [];

    % nastêpnie z pomoc± ¶redniej wa¿onej odrzucamy niepe³ne próbki
    unique_values = unique(tabela(:,3));
    unique_values = cat(2,unique_values,zeros(size(unique_values,1),2));
    for i=1:size(unique_values,1)
        [row, ~] = find(tabela(:,3) == unique_values(i));
        importance = size(row,1);
        unique_values(i,2) = importance;
        unique_values(i,3) = mean(tabela(row,2));
    end
    weighted_average = (unique_values(:,2)'*unique_values(:,3))/sum(unique_values(:,2));
    indexes = [];
    tolerance_factor = 0.12; % - 12% 

    for i=1:size(unique_values,1)
        min_value = weighted_average * (1 - tolerance_factor);
        if unique_values(i,3) < min_value
            indexes = cat(1,indexes,unique_values(i,1));
        end
    end

    for i=1:size(indexes,1)
        [row, ~] = find(tabela(:,3) == indexes(i));
        tabela(row,:) = [];
    end
    
    unique_values = unique(tabela(:,3));
    unique_values = cat(2,unique_values,zeros(size(unique_values,1),2));
    for i=1:size(unique_values,1)
        [row, ~] = find(tabela(:,3) == unique_values(i));
        importance = size(row,1);
        unique_values(i,2) = importance;
        unique_values(i,3) = mean(tabela(row,2));
    end

    % choosing one sample by weighted_average
    weighted_average = (unique_values(:,2)'*unique_values(:,3))/sum(unique_values(:,2));
    [~, row] = min(abs(unique_values(:,3)-weighted_average));
    [row, ~] = find(tabela(:,3) == unique_values(row,1));
    final_table = tabela(row,:);
    
    % counting length
    for i=1:size(final_table,1)
        [row, ~] = find(table_1(:,2) == final_table(i,1));
        indexes = table_1(row, 1);
        [row, col] = ind2sub([size(I,1) size(I,2)], indexes(:,1));
        num_pix_in_row = 0;
        num_pix_in_col = 0;
        num_pix_diag = 0;
        
        for j=1:size(indexes,1)
            tmp_row = row(j,1);
            tmp_col = col(j,1);
            % checking neighbors with order ind indexes
            if ismember(sub2ind([size(I,1) size(I,2)], tmp_row-1, tmp_col-1),indexes)
                num_pix_diag = num_pix_diag + 1;
            elseif ismember(sub2ind([size(I,1) size(I,2)], tmp_row, tmp_col-1) ,indexes)
                num_pix_in_row = num_pix_in_row + 1;
            elseif ismember(sub2ind([size(I,1) size(I,2)], tmp_row+1, tmp_col-1), indexes)
                num_pix_diag = num_pix_diag + 1;
            elseif ismember(sub2ind([size(I,1) size(I,2)], tmp_row-1, tmp_col) ,indexes)
                num_pix_in_col = num_pix_in_col + 1;
            elseif ismember(sub2ind([size(I,1) size(I,2)], tmp_row+1, tmp_col) ,indexes)
                num_pix_in_col = num_pix_in_col + 1;
            elseif ismember(sub2ind([size(I,1) size(I,2)], tmp_row-1, tmp_col+1) ,indexes)
                num_pix_diag = num_pix_diag + 1;
            elseif ismember(sub2ind([size(I,1) size(I,2)], tmp_row, tmp_col+1) ,indexes)
                num_pix_in_row = num_pix_in_row + 1;
            elseif ismember(sub2ind([size(I,1) size(I,2)], tmp_row+1, tmp_col+1) ,indexes)
                num_pix_diag = num_pix_diag + 1;
            end
        end 
        length_of_one_pixel = sqrt((75 * 25) / (size(I,1) * size(I,2)));
        length_of_rod = (num_pix_in_row+num_pix_in_col)*length_of_one_pixel + num_pix_diag*sqrt(2)*length_of_one_pixel;
        final_table(i,2) = length_of_rod;
        final_table(i,1) = i;
    end
end

function [unique_groups] = save_image(I, indexes_1, indexes_2, table, imageDestination) 
    sample_number = table(1,3);
    color_image = 255 * repmat(uint8(I), 1, 1, 3);
    
    [row, ~] = find(indexes_2(:,2) == sample_number);
    indexes_with_sample_number = indexes_2(row,:);
    all_indexes_with_sample_number = [];
    for i=1:size(indexes_with_sample_number,1)
        [row, ~] = find(indexes_1(:,1) == indexes_with_sample_number(i,1));
        all_indexes_with_sample_number = cat(1,all_indexes_with_sample_number,indexes_1(row,:));
    end

    unique_values = unique(all_indexes_with_sample_number(:,2));
    unique_values = cat(2,unique_values,zeros(size(unique_values,1),1));
    
    for i=1:size(unique_values,1)
        [row, ~] = find(all_indexes_with_sample_number(:,2) == unique_values(i));
        importance = size(row,1);
        unique_values(i,2) = importance;
    end

    groups_to_delete = [];
    for i=1:size(unique_values,1)
        if unique_values(i,2) < 15
            groups_to_delete = cat(1,groups_to_delete,unique_values(i,1));
        end
    end

    indexes_to_delete = [];
    for i=1:size(groups_to_delete,1)
        [row, ~] = find(all_indexes_with_sample_number(:,2) == groups_to_delete(i,1));
        indexes_to_delete = cat(1,indexes_to_delete,row);
    end

    all_indexes_with_sample_number(indexes_to_delete,:) = [];
    
    
    unique_groups = unique(all_indexes_with_sample_number(:,2));
    unique_groups = cat(2,unique_groups,zeros(size(unique_groups,1),3));
    min_row = size(I,1);
    max_row = 0;
    min_col = size(I,2);
    max_col = 0;
    for i=1:size(unique_groups,1)
        % randomowe kolory od 50 do 200
        a = 50;
        b = 250;
        red = floor((b-a).*rand(1,1) + a);
        green = floor((b-a).*rand(1,1) + a);
        blue = floor((b-a).*rand(1,1) + a);
        unique_groups(i,2) = red;
        unique_groups(i,3) = green;
        unique_groups(i,4) = blue;
        
        [row, ~] = find(all_indexes_with_sample_number(:,2) == unique_groups(i,1));
        indexes = all_indexes_with_sample_number(row,:);
        [row, col] = ind2sub([size(I,1) size(I,2)], indexes(:,1));
        rows_cols = [];
        rows_cols = cat(2,rows_cols,[row col]);
        for j=1:size(rows_cols,1)
            color_image(rows_cols(j,1), rows_cols(j,2),1) = red;
            color_image(rows_cols(j,1), rows_cols(j,2),2) = green;
            color_image(rows_cols(j,1), rows_cols(j,2),3) = blue;
        end
        
        if min(rows_cols(:,1)) < min_row 
            min_row = min(rows_cols(:,1));
        end
        if max(rows_cols(:,1)) > max_row
            max_row = max(rows_cols(:,1));
        end
        if min(rows_cols(:,2)) < min_col
            min_col = min(rows_cols(:,2));
        end
        if max(rows_cols(:,2)) > max_col
           max_col = max(rows_cols(:,2)); 
        end
    end

    min_row = min_row-50;
    max_row = max_row+50;
    min_col = min_col-50;
    max_col = max_col+50;
    
    color_image(min_row:min_row+5,min_col:max_col,1) = 255;
    color_image(max_row-5:max_row,min_col:max_col,1) = 255;
    color_image(min_row:max_row,min_col:min_col+5,1) = 255;
    color_image(min_row:max_row,max_col-5:max_col,1) = 255;
    
    imwrite(color_image,imageDestination);
end
