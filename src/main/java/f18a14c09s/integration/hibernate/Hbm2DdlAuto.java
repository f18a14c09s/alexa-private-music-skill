package f18a14c09s.integration.hibernate;

public enum Hbm2DdlAuto {
    validate,
    update,
    create,
    createDrop("create-drop");
    private String nameOverride;

    Hbm2DdlAuto() {
        this.nameOverride = null;
    }

    Hbm2DdlAuto(String nameOverride) {
        this.nameOverride = nameOverride;
    }

    public String value() {
        return nameOverride == null ? name() : nameOverride;
    }
}
