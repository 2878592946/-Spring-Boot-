package com.xqk.campusfind.controller;

import com.xqk.campusfind.common.Result;
import com.xqk.campusfind.dto.MatchResult;
import com.xqk.campusfind.entity.Item;
import com.xqk.campusfind.security.SecurityUtils;
import com.xqk.campusfind.service.ItemService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.Resource;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/item")
public class ItemController {

    @Resource
    private ItemService itemService;

    @Resource
    private MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.public-url}")
    private String publicUrl;

    @PostMapping("/add")
    public Result<Item> add(@RequestBody Item item) {
        // 🔒 以令牌中的当前用户为发布者，忽略前端传入的 userId，防止伪造归属
        item.setUserId(SecurityUtils.getCurrentUserId());
        Item savedItem = itemService.addItem(item);
        return Result.success(savedItem);
    }

    @GetMapping("/list")
    public Result<List<Item>> list() {
        return Result.success(itemService.getPublicItemList());
    }

    @GetMapping("/pending")
    public Result<List<Item>> pending() {
        return Result.success(itemService.getPendingList());
    }

    @PostMapping("/review")
    public Result<String> review(@RequestParam Long id, @RequestParam Integer status) {
        boolean success = itemService.reviewItem(id, status);
        return success ? Result.success("审核处理成功") : Result.error("操作失败，该物品不存在");
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return Result.success(itemService.getDashboardStats());
    }

    // 🌟 1. 注入 MinIO 客户端与桶/访问前缀配置（见上方字段）

    // ✅ 图片上传接口：改为上传到 MinIO 对象存储，返回公开读直链
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        // 生成唯一文件名防止覆盖
        String originalFilename = file.getOriginalFilename();
        String ext = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String newFileName = UUID.randomUUID().toString() + ext;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(newFileName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType() != null ? file.getContentType() : "image/jpeg")
                    .build());

            // 拼接公开读直链返回给前端（桶已设为 public-read，可直接访问）
            String fileUrl = publicUrl + "/" + newFileName;
            return Result.success(fileUrl);
        } catch (Exception e) {
            log.error("图片上传到 MinIO 失败", e);
            return Result.error("图片上传失败，对象存储异常");
        }
    }

    @PostMapping("/match")
    public Result<List<MatchResult>> smartMatch(@RequestBody Item item) {
        List<MatchResult> matches = itemService.smartMatch(item);
        return Result.success(matches);
    }
    /**
     * 获取当前用户发布的物品列表（userId 取自令牌）
     */
    @GetMapping("/my")
    public Result<List<Item>> getMyItems() {
        List<Item> items = itemService.getMyItems(SecurityUtils.getCurrentUserId());
        return Result.success(items);
    }

    /**
     * 删除物品（只有发布者本人可以删除，userId 取自令牌）
     */
    @DeleteMapping("/delete/{id}")
    public Result<String> deleteItem(@PathVariable Long id) {
        boolean success = itemService.deleteItem(id, SecurityUtils.getCurrentUserId());
        return success ? Result.success("删除成功") : Result.error("删除失败，您无权操作此物品");
    }

}