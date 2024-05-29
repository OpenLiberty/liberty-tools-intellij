/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.it.beanvalidation;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.BaseJakartaTest;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

import static io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert.*;

@RunWith(JUnit4.class)
public class BeanValidationTest extends BaseJakartaTest {

    @Test
    public void validFieldConstraints() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/beanvalidation/ValidConstraints.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // should be no errors 
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void fieldConstraintValidation() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/beanvalidation/FieldConstraintValidation.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostics
        Diagnostic d1 = d(10, 16, 23,
                "The @AssertTrue annotation can only be used on boolean and Boolean type fields.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "FixTypeOfElement", "jakarta.validation.constraints.AssertTrue");
        Diagnostic d2 = d(13, 19, 24,
                "The @AssertFalse annotation can only be used on boolean and Boolean type fields.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "FixTypeOfElement", "jakarta.validation.constraints.AssertFalse");
        Diagnostic d3 = d(17, 19, 29,
                "The @DecimalMax annotation can only be used on: \n"
                        + "- BigDecimal \n"
                        + "- BigInteger \n"
                        + "- CharSequence\n"
                        + "- byte, short, int, long (and their respective wrappers) \n"
                        + " type fields.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "FixTypeOfElement", "jakarta.validation.constraints.DecimalMax");
        Diagnostic d4 = d(17, 19, 29,
                "The @DecimalMin annotation can only be used on: \n"
                        + "- BigDecimal \n"
                        + "- BigInteger \n"
                        + "- CharSequence\n"
                        + "- byte, short, int, long (and their respective wrappers) \n"
                        + " type fields.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "FixTypeOfElement", "jakarta.validation.constraints.DecimalMin");
        Diagnostic d5 = d(20, 20, 26,
                "The @Digits annotation can only be used on: \n"
                        + "- BigDecimal \n"
                        + "- BigInteger \n"
                        + "- CharSequence\n"
                        + "- byte, short, int, long (and their respective wrappers) \n"
                        + " type fields.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "FixTypeOfElement", "jakarta.validation.constraints.Digits");
        Diagnostic d6 = d(23, 20, 32,
                "The @Email annotation can only be used on String and CharSequence type fields.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "FixTypeOfElement", "jakarta.validation.constraints.Email");
        Diagnostic d7 = d(26, 20, 34,
                "The @FutureOrPresent annotation can only be used on: Date, Calendar, Instant, LocalDate, LocalDateTime, LocalTime, MonthDay, OffsetDateTime, OffsetTime, Year, YearMonth, ZonedDateTime, HijrahDate, JapaneseDate, JapaneseDate, MinguoDate and ThaiBuddhistDate type fields.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "FixTypeOfElement", "jakarta.validation.constraints.FutureOrPresent");
        Diagnostic d8 = d(29, 19, 30,
                "The @Future annotation can only be used on: Date, Calendar, Instant, LocalDate, LocalDateTime, LocalTime, MonthDay, OffsetDateTime, OffsetTime, Year, YearMonth, ZonedDateTime, HijrahDate, JapaneseDate, JapaneseDate, MinguoDate and ThaiBuddhistDate type fields.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "FixTypeOfElement", "jakarta.validation.constraints.Future");
        Diagnostic d9 = d(33, 20, 23,
                "The @Min annotation can only be used on \n"
                        + "- BigDecimal \n"
                        + "- BigInteger\n"
                        + "- byte, short, int, long (and their respective wrappers) \n"
                        + " type fields.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "FixTypeOfElement", "jakarta.validation.constraints.Min");
        Diagnostic d10 = d(33, 20, 23,
                "The @Max annotation can only be used on \n"
                        + "- BigDecimal \n"
                        + "- BigInteger\n"
                        + "- byte, short, int, long (and their respective wrappers) \n"
                        + " type fields.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "FixTypeOfElement", "jakarta.validation.constraints.Max");
        Diagnostic d11 = d(36, 20, 27,
                "The @Negative annotation can only be used on \n"
                        + "- BigDecimal \n"
                        + "- BigInteger\n"
                        + "- byte, short, int, long, float, double (and their respective wrappers) \n"
                        + " type fields.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "FixTypeOfElement", "jakarta.validation.constraints.Negative");
        Diagnostic d12 = d(39, 19, 25,
                "The @NegativeOrZero annotation can only be used on \n"
                        + "- BigDecimal \n"
                        + "- BigInteger\n"
                        + "- byte, short, int, long, float, double (and their respective wrappers) \n"
                        + " type fields.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "FixTypeOfElement", "jakarta.validation.constraints.NegativeOrZero");
        Diagnostic d13 = d(42, 20, 32,
                "The @NotBlank annotation can only be used on String and CharSequence type fields.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "FixTypeOfElement", "jakarta.validation.constraints.NotBlank");
        Diagnostic d14 = d(45, 21, 31,
                "The @Pattern annotation can only be used on String and CharSequence type fields.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "FixTypeOfElement", "jakarta.validation.constraints.Pattern");
        Diagnostic d15 = d(48, 19, 33,
                "The @Past annotation can only be used on: Date, Calendar, Instant, LocalDate, LocalDateTime, LocalTime, MonthDay, OffsetDateTime, OffsetTime, Year, YearMonth, ZonedDateTime, HijrahDate, JapaneseDate, JapaneseDate, MinguoDate and ThaiBuddhistDate type fields.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "FixTypeOfElement", "jakarta.validation.constraints.Past");
        Diagnostic d16 = d(51, 19, 33,
                "The @PastOrPresent annotation can only be used on: Date, Calendar, Instant, LocalDate, LocalDateTime, LocalTime, MonthDay, OffsetDateTime, OffsetTime, Year, YearMonth, ZonedDateTime, HijrahDate, JapaneseDate, JapaneseDate, MinguoDate and ThaiBuddhistDate type fields.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "FixTypeOfElement", "jakarta.validation.constraints.PastOrPresent");
        Diagnostic d17 = d(54, 21, 25,
                "The @Positive annotation can only be used on \n"
                        + "- BigDecimal \n"
                        + "- BigInteger\n"
                        + "- byte, short, int, long, float, double (and their respective wrappers) \n"
                        + " type fields.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "FixTypeOfElement", "jakarta.validation.constraints.Positive");
        Diagnostic d18 = d(57, 25, 34,
                "The @PositiveOrZero annotation can only be used on \n- BigDecimal \n- BigInteger\n- byte, short, int, long, float, double (and their respective wrappers) \n type fields.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "FixTypeOfElement", "jakarta.validation.constraints.PositiveOrZero");
        Diagnostic d19 = d(60, 27, 36,
                "Constraint annotations are not allowed on static fields.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "MakeNotStatic", "jakarta.validation.constraints.AssertTrue");
        Diagnostic d20 = d(63, 27, 36,
                "Constraint annotations are not allowed on static fields.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "MakeNotStatic", "jakarta.validation.constraints.Past");

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3, d4, d5, d6, d7, d8,
                d9, d10, d11, d12, d13, d14, d15, d16, d17, d18, d19, d20);

        // Test quickfix codeActions - type (1-17), static, static+type (should only display static)
        String newText = "package io.openliberty.sample.jakarta.beanvalidation;\n\nimport java.util.Calendar;\n" +
                "import java.util.List;\n\nimport jakarta.validation.constraints.*;\n\npublic class FieldConstraintValidation {\n\n" +
                "    private int isHappy;                    // invalid types\n\n    @AssertFalse\n    private Double isSad;\n\n" +
                "    @DecimalMax(\"30.0\")\n    @DecimalMin(\"10.0\")\n    private String bigDecimal;\n\n" +
                "    @Digits(fraction = 0, integer = 0)\n    private boolean digits;\n\n    @Email\n    private Integer emailAddress;\n\n" +
                "    @FutureOrPresent\n    private boolean graduationDate;\n\n    @Future\n    private double fergiesYear;\n\n" +
                "    @Min(value = 50)\n    @Max(value = 100)\n    private boolean gpa;\n\n    @Negative\n    private boolean subZero;\n\n" +
                "    @NegativeOrZero\n    private String notPos;\n\n    @NotBlank\n    private boolean saysomething;\n\n" +
                "    @Pattern(regexp = \"\")\n    private Calendar thisIsUsed;\n\n    @Past\n    private double theGoodOldDays;\n\n" +
                "    @PastOrPresent\n    private char[] aGoodFieldName;\n\n    @Positive\n    private String[] area;\n\n" +
                "    @PositiveOrZero\n    private List<String> maybeZero;\n\n    @AssertTrue\n" +
                "    private static boolean typeValid;       // static\n\n    @Past\n" +
                "    private static boolean doubleBad;      // static and invalid type\n}";

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d1);
        TextEdit te = te(0, 0, 64, 1, newText);
        CodeAction ca = ca(uri, "Remove constraint annotation jakarta.validation.constraints.AssertTrue from element", d1, te);

        assertJavaCodeAction(codeActionParams, utils, ca);

        String newText6 = "package io.openliberty.sample.jakarta.beanvalidation;\n\nimport java.util.Calendar;\nimport java.util.List;\n\n" +
                "import jakarta.validation.constraints.*;\n\npublic class FieldConstraintValidation {\n\n    @AssertTrue\n" +
                "    private int isHappy;                    // invalid types\n\n    @AssertFalse\n    private Double isSad;\n\n" +
                "    @DecimalMax(\"30.0\")\n    @DecimalMin(\"10.0\")\n    private String bigDecimal;\n\n" +
                "    @Digits(fraction = 0, integer = 0)\n    private boolean digits;\n\n    @Email\n    private Integer emailAddress;\n\n" +
                "    @FutureOrPresent\n    private boolean graduationDate;\n\n    @Future\n    private double fergiesYear;\n\n" +
                "    @Min(value = 50)\n    @Max(value = 100)\n    private boolean gpa;\n\n    @Negative\n    private boolean subZero;\n\n" +
                "    @NegativeOrZero\n    private String notPos;\n\n    @NotBlank\n    private boolean saysomething;\n\n" +
                "    @Pattern(regexp = \"\")\n    private Calendar thisIsUsed;\n\n    @Past\n    private double theGoodOldDays;\n\n" +
                "    @PastOrPresent\n    private char[] aGoodFieldName;\n\n    @Positive\n    private String[] area;\n\n    @PositiveOrZero\n" +
                "    private List<String> maybeZero;\n\n    private static boolean typeValid;       // static\n\n    @Past\n" +
                "    private static boolean doubleBad;      // static and invalid type\n}";
        String newText7 = "package io.openliberty.sample.jakarta.beanvalidation;\n\nimport java.util.Calendar;\nimport java.util.List;\n\n" +
                "import jakarta.validation.constraints.*;\n\npublic class FieldConstraintValidation {\n\n    @AssertTrue\n" +
                "    private int isHappy;                    // invalid types\n\n    @AssertFalse\n    private Double isSad;\n\n" +
                "    @DecimalMax(\"30.0\")\n    @DecimalMin(\"10.0\")\n    private String bigDecimal;\n\n" +
                "    @Digits(fraction = 0, integer = 0)\n    private boolean digits;\n\n    @Email\n    private Integer emailAddress;\n\n" +
                "    @FutureOrPresent\n    private boolean graduationDate;\n\n    @Future\n    private double fergiesYear;\n\n" +
                "    @Min(value = 50)\n    @Max(value = 100)\n    private boolean gpa;\n\n    @Negative\n    private boolean subZero;\n\n" +
                "    @NegativeOrZero\n    private String notPos;\n\n    @NotBlank\n    private boolean saysomething;\n\n" +
                "    @Pattern(regexp = \"\")\n    private Calendar thisIsUsed;\n\n    @Past\n    private double theGoodOldDays;\n\n" +
                "    @PastOrPresent\n    private char[] aGoodFieldName;\n\n    @Positive\n    private String[] area;\n\n" +
                "    @PositiveOrZero\n    private List<String> maybeZero;\n\n    @AssertTrue\n    private boolean typeValid;       // static\n\n" +
                "    @Past\n    private static boolean doubleBad;      // static and invalid type\n}";

        JakartaJavaCodeActionParams codeActionParams2 = createCodeActionParams(uri, d19);
        TextEdit te1 = te(0, 0, 64, 1, newText6);
        TextEdit te2 = te(0, 0, 64, 1, newText7);
        CodeAction ca1 = ca(uri, "Remove constraint annotation jakarta.validation.constraints.AssertTrue from element", d19, te1);
        CodeAction ca2 = ca(uri, "Remove static modifier from element", d19, te2);

        assertJavaCodeAction(codeActionParams2, utils, ca1, ca2);

        String newText8 = "package io.openliberty.sample.jakarta.beanvalidation;\n\nimport java.util.Calendar;\nimport java.util.List;\n\n" +
                "import jakarta.validation.constraints.*;\n\npublic class FieldConstraintValidation {\n\n    @AssertTrue\n" +
                "    private int isHappy;                    // invalid types\n\n    @AssertFalse\n    private Double isSad;\n\n" +
                "    @DecimalMax(\"30.0\")\n    @DecimalMin(\"10.0\")\n    private String bigDecimal;\n\n" +
                "    @Digits(fraction = 0, integer = 0)\n    private boolean digits;\n\n" +
                "    @Email\n    private Integer emailAddress;\n\n    @FutureOrPresent\n    private boolean graduationDate;\n\n" +
                "    @Future\n    private double fergiesYear;\n\n    @Min(value = 50)\n    @Max(value = 100)\n    private boolean gpa;\n\n" +
                "    @Negative\n    private boolean subZero;\n\n    @NegativeOrZero\n    private String notPos;\n\n" +
                "    @NotBlank\n    private boolean saysomething;\n\n    @Pattern(regexp = \"\")\n    private Calendar thisIsUsed;\n\n" +
                "    @Past\n    private double theGoodOldDays;\n\n    @PastOrPresent\n    private char[] aGoodFieldName;\n\n" +
                "    @Positive\n    private String[] area;\n\n    @PositiveOrZero\n    private List<String> maybeZero;\n\n" +
                "    @AssertTrue\n    private static boolean typeValid;       // static\n\n" +
                "    private static boolean doubleBad;      // static and invalid type\n}";
        String newText9 = "package io.openliberty.sample.jakarta.beanvalidation;\n\nimport java.util.Calendar;\nimport java.util.List;\n\n" +
                "import jakarta.validation.constraints.*;\n\npublic class FieldConstraintValidation {\n\n" +
                "    @AssertTrue\n    private int isHappy;                    // invalid types\n\n    @AssertFalse\n" +
                "    private Double isSad;\n\n    @DecimalMax(\"30.0\")\n    @DecimalMin(\"10.0\")\n" +
                "    private String bigDecimal;\n\n    @Digits(fraction = 0, integer = 0)\n    private boolean digits;\n\n" +
                "    @Email\n    private Integer emailAddress;\n\n    @FutureOrPresent\n    private boolean graduationDate;\n\n" +
                "    @Future\n    private double fergiesYear;\n\n    @Min(value = 50)\n    @Max(value = 100)\n    private boolean gpa;\n\n" +
                "    @Negative\n    private boolean subZero;\n\n    @NegativeOrZero\n    private String notPos;\n\n" +
                "    @NotBlank\n    private boolean saysomething;\n\n    @Pattern(regexp = \"\")\n    private Calendar thisIsUsed;\n\n" +
                "    @Past\n    private double theGoodOldDays;\n\n    @PastOrPresent\n    private char[] aGoodFieldName;\n\n" +
                "    @Positive\n    private String[] area;\n\n    @PositiveOrZero\n    private List<String> maybeZero;\n\n" +
                "    @AssertTrue\n    private static boolean typeValid;       // static\n\n" +
                "    @Past\n    private boolean doubleBad;      // static and invalid type\n}";

        JakartaJavaCodeActionParams codeActionParams3 = createCodeActionParams(uri, d20);
        TextEdit te3 = te(0, 0, 64, 1, newText8);
        TextEdit te4 = te(0, 0, 64, 1, newText9);
        CodeAction ca3 = ca(uri, "Remove constraint annotation jakarta.validation.constraints.Past from element", d20, te3);
        CodeAction ca4 = ca(uri, "Remove static modifier from element", d20, te4);

        assertJavaCodeAction(codeActionParams3, utils, ca3, ca4);

        String newText10 = "package io.openliberty.sample.jakarta.beanvalidation;\n\nimport java.util.Calendar;\n" +
                "import java.util.List;\n\nimport jakarta.validation.constraints.*;\n\n" +
                "public class FieldConstraintValidation {\n\n    @AssertTrue\n    private int isHappy;                    // invalid types\n\n" +
                "    @AssertFalse\n    private Double isSad;\n\n    @DecimalMax(\"30.0\")\n    @DecimalMin(\"10.0\")\n" +
                "    private String bigDecimal;\n\n    @Digits(fraction = 0, integer = 0)\n    private boolean digits;\n\n" +
                "    @Email\n    private Integer emailAddress;\n\n    @FutureOrPresent\n    private boolean graduationDate;\n\n" +
                "    @Future\n    private double fergiesYear;\n\n    @Min(value = 50)\n    @Max(value = 100)\n    private boolean gpa;\n\n" +
                "    @Negative\n    private boolean subZero;\n\n    @NegativeOrZero\n    private String notPos;\n\n    @NotBlank\n" +
                "    private boolean saysomething;\n\n    @Pattern(regexp = \"\")\n    private Calendar thisIsUsed;\n\n    @Past\n" +
                "    private double theGoodOldDays;\n\n    @PastOrPresent\n    private char[] aGoodFieldName;\n\n    @Positive\n" +
                "    private String[] area;\n\n    private List<String> maybeZero;\n\n" +
                "    @AssertTrue\n    private static boolean typeValid;       // static\n\n    @Past\n" +
                "    private static boolean doubleBad;      // static and invalid type\n}";

        JakartaJavaCodeActionParams codeActionParams4 = createCodeActionParams(uri, d18);
        TextEdit te5 = te(0, 0, 64, 1, newText10);
        CodeAction ca5 = ca(uri, "Remove constraint annotation jakarta.validation.constraints.PositiveOrZero from element", d1, te5);

        assertJavaCodeAction(codeActionParams4, utils, ca5);
    }

    @Test
    public void methodConstraintValidation() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/beanvalidation/MethodConstraintValidation.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostics
        Diagnostic d1 = d(20, 26, 38,
                "Constraint annotations are not allowed on static methods.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "MakeNotStatic", "jakarta.validation.constraints.AssertTrue");
        Diagnostic d2 = d(25, 18, 28,
                "The @AssertTrue annotation can only be used on boolean and Boolean type methods.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "FixTypeOfElement", "jakarta.validation.constraints.AssertTrue");
        Diagnostic d3 = d(30, 23, 33,
                "Constraint annotations are not allowed on static methods.",
                DiagnosticSeverity.Error, "jakarta-bean-validation", "MakeNotStatic", "jakarta.validation.constraints.AssertFalse");

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3);

        // Test quickfix codeAction
        String newText1 = "package io.openliberty.sample.jakarta.beanvalidation;\n\nimport jakarta.validation.constraints.AssertFalse;\nimport jakarta.validation.constraints.AssertTrue;\n\npublic class MethodConstraintValidation {\n\n    // valid cases\n    @AssertFalse\n    private boolean falseMethod() {\n        return false;\n    }\n\n    @AssertTrue\n    public boolean trueMethod() {\n        return true;\n    }\n\n    // invalid cases\n    public static boolean anotherTruth() {  // static\n        return true;\n    }\n\n    @AssertTrue\n    public String notBoolean() {            // invalid type\n        return \"aha!\";\n    }\n\n    @AssertFalse\n    private static int notBoolTwo(int x) {  // invalid type, static\n        return x;\n    }\n   \n}";


        String newText2 = "package io.openliberty.sample.jakarta.beanvalidation;\n\nimport jakarta.validation.constraints.AssertFalse;\n" +
                "import jakarta.validation.constraints.AssertTrue;\n\npublic class MethodConstraintValidation {\n\n" +
                "    // valid cases\n    @AssertFalse\n    private boolean falseMethod() {\n        return false;\n    }\n\n" +
                "    @AssertTrue\n    public boolean trueMethod() {\n        return true;\n    }\n\n" +
                "    // invalid cases\n    @AssertTrue\n    public boolean anotherTruth() {  // static\n        return true;\n    }\n\n" +
                "    @AssertTrue\n    public String notBoolean() {            // invalid type\n        return \"aha!\";\n    }\n\n" +
                "    @AssertFalse\n    private static int notBoolTwo(int x) {  // invalid type, static\n        return x;\n    }\n\n}";

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d1);
        TextEdit te = te(0, 0, 34, 1, newText1);
        TextEdit te2 = te(0, 0, 34, 1, newText2);
        CodeAction ca = ca(uri, "Remove constraint annotation jakarta.validation.constraints.AssertTrue from element", d1, te);
        CodeAction ca2 = ca(uri, "Remove static modifier from element", d1, te2);

        assertJavaCodeAction(codeActionParams, utils, ca, ca2);

        String newText3 = "package io.openliberty.sample.jakarta.beanvalidation;\n\nimport jakarta.validation.constraints.AssertFalse;\n" +
                "import jakarta.validation.constraints.AssertTrue;\n\npublic class MethodConstraintValidation {\n\n    // valid cases\n" +
                "    @AssertFalse\n    private boolean falseMethod() {\n        return false;\n    }\n\n" +
                "    @AssertTrue\n    public boolean trueMethod() {\n        return true;\n    }\n\n    // invalid cases\n" +
                "    @AssertTrue\n    public static boolean anotherTruth() {  // static\n        return true;\n    }\n\n" +
                "    public String notBoolean() {            // invalid type\n        return \"aha!\";\n    }\n\n    @AssertFalse\n" +
                "    private static int notBoolTwo(int x) {  // invalid type, static\n        return x;\n    }\n   \n}";

        codeActionParams = createCodeActionParams(uri, d2);
        te = te(0, 0, 34, 1, newText3);
        ca = ca(uri, "Remove constraint annotation jakarta.validation.constraints.AssertTrue from element", d2, te);

        assertJavaCodeAction(codeActionParams, utils, ca);

        String newText4 = "package io.openliberty.sample.jakarta.beanvalidation;\n\nimport jakarta.validation.constraints.AssertFalse;\n" +
                "import jakarta.validation.constraints.AssertTrue;\n\npublic class MethodConstraintValidation {\n\n" +
                "    // valid cases\n    @AssertFalse\n    private boolean falseMethod() {\n        return false;\n    }\n\n" +
                "    @AssertTrue\n    public boolean trueMethod() {\n        return true;\n    }\n\n    // invalid cases\n    @AssertTrue\n" +
                "    public static boolean anotherTruth() {  // static\n        return true;\n    }\n\n    @AssertTrue\n" +
                "    public String notBoolean() {            // invalid type\n        return \"aha!\";\n    }\n\n" +
                "    private static int notBoolTwo(int x) {  // invalid type, static\n        return x;\n    }\n   \n}";

        String newText5 = "package io.openliberty.sample.jakarta.beanvalidation;\n\n" +
                "import jakarta.validation.constraints.AssertFalse;\nimport jakarta.validation.constraints.AssertTrue;\n\n" +
                "public class MethodConstraintValidation {\n\n    // valid cases\n    @AssertFalse\n    private boolean falseMethod() {\n" +
                "        return false;\n    }\n\n    @AssertTrue\n    public boolean trueMethod() {\n        return true;\n    }\n\n" +
                "    // invalid cases\n    @AssertTrue\n    public static boolean anotherTruth() {  // static\n" +
                "        return true;\n    }\n\n    @AssertTrue\n    public String notBoolean() {            // invalid type\n" +
                "        return \"aha!\";\n    }\n\n    @AssertFalse\n    private int notBoolTwo(int x) {  // invalid type, static\n" +
                "        return x;\n    }\n\n}";

        codeActionParams = createCodeActionParams(uri, d3);
        te = te(0, 0, 34, 1, newText4);
        te2 = te(0, 0, 34, 1, newText5);
        ca = ca(uri, "Remove constraint annotation jakarta.validation.constraints.AssertFalse from element", d3, te);
        ca2 = ca(uri, "Remove static modifier from element", d3, te2);

        assertJavaCodeAction(codeActionParams, utils, ca, ca2);
    }
}
