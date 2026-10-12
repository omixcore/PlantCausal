# PlantCausal

A causal inference framework for plant multi-omics using target trial emulation, causal discovery, and virtual intervention analysis.

## Current implementation: Core 0.1.0

Java 17 数据模型、校验服务与首批公开数据 benchmark。以 PlantCausal Data Standard v1.0.0 草案为基础；本版本专注数据质量与设计审计，不输出因果效应。

## 在 IntelliJ IDEA 中运行

1. 解压后，选择 Open，打开本目录的 `pom.xml`，作为 Maven 项目导入。
2. Project SDK 设置为 JDK 17 或更高版本；Maven 编译目标固定为 Java 17。
3. 运行 `org.plantcausal.core.Main`，Program arguments 填写 `validate examples/synthetic`。
4. Working directory 设置为此项目根目录。
5. 运行真实数据时，参数改为 `validate benchmark/standardized/PC_B001`。

首次 Maven 导入需要网络下载依赖。构建后的 `target/plantcausal-core-0.1.0.jar` 可直接用 Java 17 运行。GitHub Actions 会构建并提供 JAR 下载。

```sh
mvn test
mvn package
java -jar target/plantcausal-core-0.1.0.jar validate examples/synthetic
java -jar target/plantcausal-core-0.1.0.jar validate benchmark/standardized/PC_B001
```

CLI 将报告打印为 JSON。退出码：0=格式与关系校验通过（可有警告），1=校验不通过，2=用法错误。导入/下载异常退出非零。`unitCount` 仅为记录数，不代表独立重复数。

## 软件结构与接口

| 类 | 作用 |
|---|---|
| Models | Protocol、TargetTrial、Estimand、ExperimentalUnit、VariableDefinition、Observation、TreatmentInterval、Event、ValidationReport |
| PackageReader | 严格 JSON 与支持引号的 UTF-8 TSV 导入 |
| ValidationService | 内置 JSON Schema 2020-12、关系与时间规则校验；可直接嵌入 GUI/服务 |
| BenchmarkFetcher | 从 Zenodo 下载指定版本元数据与文件，验证仓库校验和与许可证 |
| BenchmarkImporter | 对已审核源文件进行 SHA-256 固定的 Java Excel 转换 |
| Main | 命令行入口 |

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
java -jar target/plantcausal-core-0.1.0.jar fetch-benchmark benchmark/raw
java -jar target/plantcausal-core-0.1.0.jar import-pine benchmark/raw/10100580/Mycorrhiza_exposure_drought.xlsx benchmark/standardized/PC_B001
java -jar target/plantcausal-core-0.1.0.jar stage-grass benchmark/raw/4306840/181004_Biomass_2014.xls benchmark/staging/PC_B002
```

源文件内容更改后，固定 SHA-256 的转换器拒绝处理，必须重新评审并更新适配器。抓取服务验证当前仓库 MD5，适配器再验证固定 SHA-256。首次发布随包源文件已独立下载并验证；Java 在线抓取需要用户环境网络，本次主要验证离线转换与校验。

## 草案兼容性说明

先前草案要求 time_zero 为 ISO 日期；真实公开实验可能只公布相对天数。此首版实现允许 units.time_zero=NA，明确给出 UNKNOWN_CALENDAR_ZERO 警告，协议必须描述相对起点。此为草案的明确补充；没有捏造实际日期。严格要求绝对日期的下游服务应把此警告升级为阻断条件。

v1.0 核心只支持 NA 缺失码；输入其他缺失码应先显式归一化并记录转换。策略 ID 的因果对比采用 A-B，两端 ID 不应包含连字符；多重/复合对比与多并行暴露尚未支持。暴露区间是 [start, stop)，没有实际浇水日志就不能从文字方案生成伪造的实际暴露区间。

## 引用与许可

详见 `benchmark/README.md`、`benchmark/catalog.json` 和各 record.json。公开数据为 CC BY 4.0，转载数据与转换版须保留作者、DOI 和修改说明。软件使用的第三方依赖保留在构建清单中；本仓库源码采用现有 MIT 许可证；公开研究数据仍按各自的 CC BY 4.0 许可使用，MIT 不替代数据许可。
