package tn.iset.marina.consumption_service.entity;

public enum ConsumptionType {
    ELECTRICITY("kWh"),
    WATER("m3");

    private final String unit;

    ConsumptionType(String unit) {
        this.unit = unit;
    }

    public String unit() {
        return unit;
    }
}
