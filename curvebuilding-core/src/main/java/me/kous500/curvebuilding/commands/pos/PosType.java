package me.kous500.curvebuilding.commands.pos;

public class PosType {
    public static PosType get(String posStr) {
        StringBuilder builderN = new StringBuilder();

        for(String s : posStr.split("")) {
            if (s.matches("^[0-9]$")) {
                builderN.append(s);
            } else if (s.matches("[fb]")) {
                String posN = builderN.toString();
                int IntegerN;

                try {
                    IntegerN = Integer.parseInt(posN);
                } catch (NumberFormatException e) {
                    return new PosType(ErrorType.invalidInteger);
                }

                if (posN.equals("") || IntegerN <= 0) {
                    return new PosType(ErrorType.integerLess);
                }

                if (s.equals("f")) {
                    return new PosType(IntegerN, 1);
                } else if (s.equals("b")) {
                    return new PosType(IntegerN, 2);
                } else {
                    return new PosType(ErrorType.incorrectArgument);
                }
            } else {
                return new PosType(ErrorType.incorrectArgument);
            }
        }

        String posN = builderN.toString();
        int IntegerN;
        try {
            IntegerN = Integer.parseInt(posN);
        } catch (NumberFormatException e) {
            return new PosType(ErrorType.invalidInteger);
        }

        if(!posN.equals("") && IntegerN > 0) {
            return new PosType(IntegerN, 0);
        } else {
            return new PosType(ErrorType.integerLess);
        }
    }

    public int n;
    public int h;
    public ErrorType errorType;

    private PosType(int n, int h) {
        this.n = n;
        this.h = h;
    }

    private PosType(ErrorType errorType) {
        this.errorType = errorType;
    }

    public enum ErrorType {
        integerLess,
        incorrectArgument,
        invalidInteger
    }
}
