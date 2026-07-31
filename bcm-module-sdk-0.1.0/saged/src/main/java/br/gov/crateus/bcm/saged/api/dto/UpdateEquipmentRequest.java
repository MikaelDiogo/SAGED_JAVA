package br.gov.crateus.bcm.saged.api.dto;

public class UpdateEquipmentRequest {

    private Boolean isRented;
    private String assetTag;
    private String equipmentName;
    private String equipmentModel;

    public Boolean getIsRented() { return isRented; }
    public void setIsRented(Boolean isRented) { this.isRented = isRented; }

    public String getAssetTag() { return assetTag; }
    public void setAssetTag(String assetTag) { this.assetTag = assetTag; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public String getEquipmentModel() { return equipmentModel; }
    public void setEquipmentModel(String equipmentModel) { this.equipmentModel = equipmentModel; }
}
