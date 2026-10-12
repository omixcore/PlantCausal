# PlantCausal

A causal inference framework for plant multi-omics using target trial emulation, causal discovery, and virtual intervention analysis.

## Current implementation: Desktop 0.2.0

Java 17 桌面界面、数据模型、校验服务与首批公开数据 benchmark。以 PlantCausal Data Standard v1.0.0 草案为基础；本版本专注数据质量与设计审计，不输出因果效应。

## 在 IntelliJ IDEA 中打开界面

1. 克隆 https://github.com/omixcore/PlantCausal.git，打开根目录 pom.xml，作为 Maven 项目导入。
2. Project SDK 设置为 JDK 17 或更高；等待 Maven 下载依赖。
3. 打开 src/main/java/org/plantcausal/core/Main.java，直接运行 main，不填写 Program arguments，即打开桌面界面。填写 gui 也可以。
4. 点击 Load demo 或 Load public data，界面会直接显示表格。

界面采用 Java Swing，支持 Windows/macOS/Linux 的图形桌面，无需安装 JavaFX。

| 页面 | 功能 |
|---|---|
| Overview | 打开项目、内置合成示例和公开松树数据 |
| Protocol | 常用字段表单编辑、完整 JSON 编辑、协议保存 |
| Data preview | 五种 TSV 表格预览、筛选与重新载入 |
| Validation | 错误与警告表、单位和观测统计、JSON 报告导出 |
| Benchmarks | 两项公开研究数据、DOI、许可及设计限制；草地 staging 预览 |

Open project 选择含 protocol.json 和数据表的文件夹。内置示例加载到独立临时目录，使用 Save project as 保存到空文件夹以保留修改。表格当前只读；协议可编辑，TSV 可在外部编辑后 Reload。表格预览最多2000行，完整数据仍由核心校验器校验。

后台线程执行导入与校验，处理期间按钮和协议编辑被禁用。未保存协议时不能校验；切换项目或关闭窗口前会提示未保存修改。导出报告会核对输入文件 SHA-256，拒绝导出已经过期的报告。

## 构建和启动

```sh
mvn verify
java -jar target/plantcausal-core-0.2.0.jar
```

Windows 用户可以运行 start-PlantCausal.bat；脚本使用本地构建 JAR 或调用 Maven 构建，要求 Java 17。GitHub Actions 构建产物包括可执行 JAR；下载并解压后可用上述 java -jar 命令打开窗口。内置数据已在 JAR 中，无需依赖当前工作目录。

原命令行继续支持：

```sh
java -jar target/plantcausal-core-0.2.0.jar validate examples/synthetic
java -jar target/plantcausal-core-0.2.0.jar validate benchmark/standardized/PC_B001
```

CLI 将报告打印为 JSON。退出码：0=格式与关系校验通过（可有警告），1=校验不通过，2=用法错误。unitCount 仅为记录数，不代表独立重复数。无图形环境请使用 CLI。

## 软件结构与接口

| 类 | 作用 |
|---|---|
| Models | Protocol、TargetTrial、Estimand、ExperimentalUnit、VariableDefinition、Observation、TreatmentInterval、Event、ValidationReport |
| PackageReader | 严格 JSON 与支持引号的 UTF-8 TSV 导入 |
| ValidationService | 内置 JSON Schema 2020-12、关系与时间规则校验；可直接嵌入 GUI/服务 |
| BenchmarkFetcher | 从 Zenodo 下载指定版本元数据与文件，验证仓库校验和与许可证 |
| BenchmarkImporter | 对已审核源文件进行 SHA-256 固定的 Java Excel 转换 |
| DesktopApp | 五个页面的 Java Swing 桌面界面 |
| WorkspaceService | 内置示例、项目另存、协议保存、预览与报告导出 |
| Main | 默认桌面启动，保留命令行入口 |

```java
var report = new ValidationService().validate(java.nio.file.Path.of("benchmark/standardized/PC_B001"));
if (!report.schemaValid()) {
    report.issues().forEach(System.out::println);
}
```

Protocol 用 typed records 保存核心协议，同时保留完整 JsonNode 以保存 emulation、identification、provenance 和 extensions。Protocol 的 JsonNode 可变，调用方不得改写它并视作已验证对象；重新调用校验服务验证任何更改。校验服务按调用重新读取文件，没有全局项目状态。表之间使用显式键，无行号隐式关联。

## 校验范围

字段与类型、重复 JSON 键、重复 TSV 表头、行长度、主键、外键、策略 ID、来源 ID、嵌套单位循环、处理层级、共享处理一致性、ISO 日期、缺失原因、数值类型、单位、主要终点覆盖、暴露区间与重叠、事件时间均有校验。持续策略缺少实际暴露历史或依从性会警告。

所有结果的 designReviewed 与 analysisReady 均为 NOT_ASSESSED；当前版本没有人工签核流程，也没有因果估计器。schemaValid 同时包含格式与跨表规则，不表示识别假设成立。任意科学文本、表达式筛选、随机化真实性、未测混杂与有效样本量仍需人工评审。

## 首批 benchmark

- PC_B001：松树幼苗干旱/恢复实验；原始工作簿与标准化包均提供。280 个单株盆单位；第10/24天蒸腾与水势，以及处理前菌根类别，转成长表。
- PC_B002：2014 年草地季节性干旱实验；已下载原始产量表，并转换为带真实收获日期的 staging 表。尚未定义可验证的处理起止映射，不能作为标准化因果输入。
- examples/synthetic：仅用于软件正向/错误校验，不是真实公开数据，也不是论文结果。

本版 benchmark 检查导入、缺失保留、逐值一致与设计限制；真实因果效应没有已知真值，不能用它评价估计器偏差/RMSE。后续算法 benchmark 应另加带已知效应的模拟或半合成数据，并明确生成机制。

## 重建数据

```sh
java -jar target/plantcausal-core-0.2.0.jar fetch-benchmark benchmark/raw
java -jar target/plantcausal-core-0.2.0.jar import-pine benchmark/raw/10100580/Mycorrhiza_exposure_drought.xlsx benchmark/standardized/PC_B001
java -jar target/plantcausal-core-0.2.0.jar stage-grass benchmark/raw/4306840/181004_Biomass_2014.xls benchmark/staging/PC_B002
```

源文件内容更改后，固定 SHA-256 的转换器拒绝处理，必须重新评审并更新适配器。抓取服务验证当前仓库 MD5，适配器再验证固定 SHA-256。首次发布随包源文件已独立下载并验证；Java 在线抓取需要用户环境网络，本次主要验证离线转换与校验。

## 草案兼容性说明

先前草案要求 time_zero 为 ISO 日期；真实公开实验可能只公布相对天数。此首版实现允许 units.time_zero=NA，明确给出 UNKNOWN_CALENDAR_ZERO 警告，协议必须描述相对起点。此为草案的明确补充；没有捏造实际日期。严格要求绝对日期的下游服务应把此警告升级为阻断条件。

v1.0 核心只支持 NA 缺失码；输入其他缺失码应先显式归一化并记录转换。策略 ID 的因果对比采用 A-B，两端 ID 不应包含连字符；多重/复合对比与多并行暴露尚未支持。暴露区间是 [start, stop)，没有实际浇水日志就不能从文字方案生成伪造的实际暴露区间。

## 引用与许可

详见 `benchmark/README.md`、`benchmark/catalog.json` 和各 record.json。公开数据为 CC BY 4.0，转载数据与转换版须保留作者、DOI 和修改说明。软件使用的第三方依赖保留在构建清单中；本仓库源码采用现有 MIT 许可证；公开研究数据仍按各自的 CC BY 4.0 许可使用，MIT 不替代数据许可。
