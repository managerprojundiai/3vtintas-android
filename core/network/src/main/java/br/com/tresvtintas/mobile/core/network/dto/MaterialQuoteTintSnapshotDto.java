package br.com.tresvtintas.mobile.core.network.dto;

public record MaterialQuoteTintSnapshotDto(
        long colorId,
        String colorPublicId,
        String colorName,
        long tintContextId,
        String tintContextPublicId,
        String lineName,
        String finishName,
        String packageName,
        String packageCode,
        String baseCode,
        String unitCode) {
    public MaterialQuoteTintSnapshotDto {
        colorId = DtoValidation.requirePositive(colorId, "Color ID");
        colorPublicId = DtoValidation.requireText(colorPublicId, "Color ID", 36);
        colorName = DtoValidation.requireText(colorName, "Color name", 160);
        tintContextId = DtoValidation.requirePositive(
                tintContextId, "Tint context ID");
        tintContextPublicId = DtoValidation.requireText(
                tintContextPublicId, "Tint context ID", 36);
        lineName = DtoValidation.requireText(lineName, "Tint line", 160);
        finishName = DtoValidation.requireText(finishName, "Tint finish", 120);
        packageName = DtoValidation.requireText(packageName, "Tint package", 120);
        packageCode = DtoValidation.requireText(packageCode, "Tint package code", 80);
        baseCode = DtoValidation.requireText(baseCode, "Tint base", 80);
        unitCode = DtoValidation.requireText(unitCode, "Tint unit", 40);
    }
}
