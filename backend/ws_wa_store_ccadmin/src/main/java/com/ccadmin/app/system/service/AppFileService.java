package com.ccadmin.app.system.service;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.model.entity.id.BusinessConfigEntityID;
import com.ccadmin.app.shared.service.BusinessConfigSearchService;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.system.model.dto.AppFileDto;
import com.ccadmin.app.system.model.entity.AppFileEntity;
import com.ccadmin.app.system.repository.AppFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AppFileService extends SessionService {

    @Autowired
    private AppFileRepository appFileRepository;
    @Autowired
    BusinessConfigSearchService businessConfigSearchService;

    public AppFileEntity findById(String FileCod)
    {
        return this.appFileRepository.findById(FileCod).get();
    }

    public ResponseWsDto save(AppFileDto appFileDto) throws IOException
    {
        AppFileEntity appFile = new AppFileEntity();

        int groupTypeFile = (appFileDto.groupTypeFile == 0) ? 1 : appFileDto.groupTypeFile;

        BusinessConfigEntity physicalRoute = this.businessConfigSearchService.findById(
                new BusinessConfigEntityID("ConfigurationFiles",groupTypeFile)
        );

        appFile.FileType = getTypeFile(appFileDto.extension);
        appFile.FileCod = generateCodFile(appFile.FileType);
        appFile.Name = appFile.FileCod + "." + appFileDto.extension;
        appFile.Route = physicalRoute.ConfigVal + appFile.Name;
        appFile.Description = "no Description";
        appFile.addSession(getUserCod());

        byte[] imageBytes = Base64.getDecoder().decode(appFileDto.base64.split(",")[1]);

        Path path = Paths.get(appFile.Route);

        Files.write(path, imageBytes);

        appFile.Route = physicalRoute.Str4Config + appFile.Name;
        return new ResponseWsDto(
                this.appFileRepository.save(appFile)
        );
    }

    public String getTypeFile(String extension){

        String[] imageExtensions = {
                "jpg", "jpeg", "png", "gif", "bmp", "tiff", "webp", "ico", "heif", "svg"
        };

        if( Arrays.stream(imageExtensions).toList().stream().filter(
                e -> e.toUpperCase().equals(extension.toUpperCase()) ).toList().size() > 0
        )
        {
            return "IMAGE";
        }

        return "OTHER";
    }

    private String generateCodFile(String typeFile){

        Map<String, String> typeMapping = new HashMap<>();
        typeMapping.put("IMAGE", "IMG");
        typeMapping.put("OTHER", "OTR");
        typeMapping.put("DOCUMENT", "DOC");
        typeMapping.put("VIDEO", "VID");

        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");
        String formattedDate = today.format(formatter);

        String suffix = typeMapping.get(typeFile);
        String baseCode = formattedDate + UUID.randomUUID().toString().replace("-","");

        if(baseCode.length()>17){
            baseCode = baseCode.substring(0,17);
        }
        while (baseCode.length() < 17) {
            baseCode = "0" + baseCode;
        }
        return suffix + baseCode;
    }


}
