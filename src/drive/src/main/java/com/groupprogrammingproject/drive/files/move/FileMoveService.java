package com.groupprogrammingproject.drive.files.move;

import com.amazonaws.services.s3.AmazonS3;
import com.groupprogrammingproject.drive.Utils;
import com.groupprogrammingproject.drive.domain.file.Item;
import com.groupprogrammingproject.drive.domain.file.ItemRepository;
import com.groupprogrammingproject.drive.domain.file.ItemType;
import com.groupprogrammingproject.drive.exception.NonexistentObjectException;
import com.groupprogrammingproject.drive.files.move.dto.FileMoveRequest;
import com.groupprogrammingproject.drive.files.move.dto.FileMoveResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileMoveService {

    private final AmazonS3 amazonS3;

    private final ItemRepository itemRepository;

    @Value("${amazon.s3.bucket}")
    private String bucketName;

    public FileMoveResponse move(FileMoveRequest fileMoveRequest) {
        try {
            Item item = itemRepository.findById(fileMoveRequest.getItemId()).get();
            Item folder = itemRepository.findById(fileMoveRequest.getNewParentItemId()).orElse(new Item());
            String parentPath = getFolderFullPathByFolderId(fileMoveRequest.getNewParentItemId());
            folder.setPath(parentPath);

            if (folder.getType() != ItemType.FOLDER && !Utils.isRootPath(parentPath)) throw new Exception("Destination need to be a folder");
            if (Utils.isRootPath(item.getPath())) throw new Exception("Cannot move root folder");
            if (item.getPath().equalsIgnoreCase(folder.getPath())) throw new Exception("Cannot move to the same folder");

            Item movedItem = switch (item.getType()) {
                case ItemType.FOLDER -> moveFolder(item, folder);
                case ItemType.FILE -> moveFile(item, folder);
                default -> null;
            };

            return new FileMoveResponse(movedItem.getPath());
        } catch (Exception e) {
            throw new NonexistentObjectException(e.getMessage());
        }
    }

    private Item moveFile(Item file, Item folder) {
        amazonS3.copyObject(bucketName, file.getPath(), bucketName, Utils.createFilePath(folder.getPath(), file.getId()));
        amazonS3.deleteObject(bucketName, file.getPath());
        file.setPath(Utils.createFilePath(folder.getPath(), file.getId()));
        return itemRepository.save(file);
    }

    @Transactional
    private Item moveFolder(Item folder, Item newFolder) {
        String newPath = Utils.createFilePath(newFolder.getPath(), folder.getId());

        List<Item> allFiles = itemRepository.findAllUnderPath(Utils.getFileKeyFromFullPath(folder.getPath()));
        List<Item> folders = allFiles.stream().filter(item -> item.getType() == ItemType.FOLDER).toList(); // .sorted(Comparator.comparingInt(s -> s.getPath().length()))
        List<Item> files = allFiles.stream().filter(item -> item.getType() == ItemType.FILE).toList();

        // folders.sort(Comparator.comparingInt(s -> s.getPath().length()));
        System.err.println("Tuuu1");
        for (Item f : folders) {
            String newFolderPath = Utils.createFilePath(newPath, f.getId()); // f.getPath().replaceFirst(folder.getPath(), newFolder.getPath());
            // amazonS3.copyObject(bucketName, f.getPath(), bucketName, newFilePath);
            // amazonS3.deleteObject(bucketName, f.getPath());

            f.setPath(newFolderPath);
            itemRepository.save(f);
        }
        System.err.println("Tuuu2");
        for (Item f : files) {
            String newFilePath = Utils.createFilePath(newPath, f.getId());// f.getPath().replaceFirst(folder.getPath(), newFolder.getPath());
            amazonS3.copyObject(bucketName, f.getPath(), bucketName, newFilePath);
            amazonS3.deleteObject(bucketName, f.getPath());

            f.setPath(newFilePath);
            itemRepository.save(f);
        }
        System.err.println("Tuuu3");

        // amazonS3.copyObject(bucketName, folder.getPath(), bucketName, newPath);
        // amazonS3.deleteObject(bucketName, folder.getPath());
        folder.setPath(newPath);

        return itemRepository.save(folder);
    }

    private String getFolderFullPathByFolderId(String parentFolder) {
        if (Utils.isRootPath(parentFolder)) {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } else {
            Item folder = itemRepository.findById(parentFolder).get();
            return folder.getPath();
        }
    }
}
