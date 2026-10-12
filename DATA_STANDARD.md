# PlantCausal Data Standard v1.0.0 — Draft

这是供 PlantCausal 实现与评审的项目草案，并非已获社区认可的标准。目标是将植物目标试验协议、公开数据映射及可审计数据结构连接起来。协议的文字字段不可直接作为可执行筛选条件；未来实现必须增加显式表达式语言及其校验器。

## 1. 设计范围

v1.0 优先支持固定基线、两组或多组策略、单个主要终点、长表形式的连续/二分类/事件数据。动态策略、连续剂量、序贯试验、跨研究运输与高维组学可在 extensions 记录，但不应宣称当前校验器支持其估计。组学特征仅在因果时间顺序和测量单位明确后进入分析；差异表达本身不能证明因果关系。

沿用 SDRF 的样本与数据关联思想，但不宣称这些 TSV 是 SDRF 兼容文件。原 SDRF 可通过后续 sample_id 映射表连接。完整因果协议放在 JSON，避免用一张宽表混合地块、植株和样本层级。

## 2. 包文件与行粒度

| 文件 | 一行/对象表示 | 必需性 |
|---|---|---|
| protocol.example.json | 一个目标试验及数据映射 | 必需；真实项目命名 protocol.json |
| protocol.schema.json | JSON Schema 2020-12 结构约束 | 必需 |
| units.tsv | 一个实验/观测单位；嵌套单位可单列 | 必需 |
| variables.tsv | 一个变量的类型、角色、单位、时间语义 | 必需 |
| observations.tsv | 一个单位×时间×变量×样本×技术重复的观测 | 必需 |
| treatments.tsv | 一个处理分配单位在一个区间的实际暴露 | 持续策略必需 |
| events.tsv | 一次失访、死亡、收获或策略改变事件 | 空表可接受；有事件则必须记录 |

所有表采用 UTF-8 TSV、首行为字段名、小数点为点。保留字符串标识符与前导零；不得根据行号隐式链接。使用 NA 表示未知/不适用；测量值缺失时 missing_reason 必须说明 not_measured/lost_to_followup/below_detection/not_applicable/unknown。NA 不等于 0。协议文本不得使用 NA 代替关键定义。

## 3. Target trial 七项与映射

资格、处理策略、分配方式、随访、结局、因果对比和分析计划均必须定义。target_trial 写理想协议；emulation 逐项写实际数据库字段、规则、可实现程度与偏差。观察性分类不得描述为随机分配，也不得自动称为 ITT。随访从符合资格及处理分配对齐的时间零点开始；不得以处理后存活或处理后表达筛选基线资格。

estimand 必须写清目标群体、策略 A/B、结局时点、效应尺度及死亡/失访/换处理等事件的处理方式。contrast=A-B 时正值始终表示 A 较高。ATT、ATE 等缩写不能替代完整 estimand 定义。

## 4. 植物实验单位与字段字典

units: trial_id 关联协议；unit_id 包内唯一；parent_unit_id 指向上层单位或 NA；unit_type 为 plant/pot/plot/chamber/site；assignment_unit_id 指向实际接受处理的单位；cluster_id 标识推断相关簇；block_id 标识设计区组；species 学名（推荐另存 NCBI Taxonomy ID）；genotype 保留品种/材料原名；time_zero ISO 8601 日期或带时区时间；eligible 为0/1；strategy_id 与协议策略关联；source_id 对应来源。

observations: time 为相对 time_zero 的数值，单位由协议指定。基线可以为负数；time=0 必须额外确认测量发生在首次处理之前。variable_id 对应字典；value 按 data_type 解析；sample_id 为生物样本；technical_replicate_id 为技术重复，不能作为独立 n；source_location 为论文页码、表格、补充文件行号或原数据记录定位。

variables: role 可为 baseline_confounder/time_varying_confounder/outcome/mediator/effect_modifier/precision_covariate/descriptive；角色针对本试验而非变量固有属性。data_type 为 number/integer/boolean/string；timing 明确处理前后；unit 对数值变量必需。推荐 MIAPPE、Plant Ontology 与 Crop Ontology 的标识符，但保留来源原始名称和单位，转换记录必须可追溯。

treatments: start/stop 为 [start,stop) 暴露区间；strategy_id 是实际对应策略，不能推定全程依从；dose 可 NA，但真实策略必须在协议明确定义剂量/阈值；dose_unit 与 dose 成对；adherence 为 adherent/nonadherent/unknown。最后时点的结局测量遵循协议规定的处理结束次序。

events: event_type 为 death/loss_to_followup/harvest/strategy_switch/other。死亡是否为结局、竞争事件或中间事件由 estimand 决定，不能一律删掉。

共享盆、地块、温室或生长箱必须明确处理分配层级。一个处理生长箱内的几十株植物不提供几十个独立处理重复。统计模型与重采样必须遵循分配/聚类结构；区组不自动等于相关簇。

## 5. 来源与公开数据入库

provenance 至少记录 source_id、source_uri、source_location、license、retrieved_at 与 transformations。多来源项目在 extensions.sources 增加来源对象列表；所有 source_id 必须可解析。建议补充 DOI、数据库 accession、文件 SHA-256、提取者与提取日期。下载访问不代表可以重新分发；许可证未知的文件只能登记来源并按许可使用。

公开数据纳入 benchmark 前，必须具备可辨认实验单位、明确暴露与结局、时间顺序及可追溯原文；缺少处理时间、混杂测量或层级重复的数据可以登记为 limited，不能自动进入 causal_ready。合成数据另标 synthetic，绝不冒充论文数据。本包全部是合成示例，没有复用以前任何研究数据。

## 6. 校验与因果就绪

三层分开报告：schema_valid（字段/类型/外键通过）、design_reviewed（研究人员确认时间、层级与协议映射）、analysis_ready（指定估计器的必要条件满足）。格式通过不证明无混杂、正值性、一致性或无干扰成立。

硬错误：重复主键、外键不存在、未定义策略、baseline_confounder 在 time>0、结局早于时间零点、区间倒置、同一单位暴露重叠、缺失值无原因。time=0 基线顺序、处理与数据覆盖、毁损取样、策略可实施性、跨地块干扰、聚类及失访均需设计评审。不能自动套用倾向评分；处理影响的时间变化混杂需要相应纵向方法。

随包 validate.py 提供最小结构与关系校验，不执行因果估计，也不检查所有科学语义。数据缺少 follow-up 不能靠填表补成纵向研究。

## 7. Java 实现边界

先建 Protocol/ExperimentalUnit/Observation/TreatmentInterval/VariableDefinition DTO，随后做 JSON Schema 验证与独立关系/时间校验，再接 CSV/TSV 导入和算法接口。JSON Schema 不能验证跨文件外键；必须由服务层实现。GUI 后续调用同一核心。未来发布版本使用语义版本号；破坏字段语义的变更升主版本。

## 8. 运行

python -m pip install jsonschema
python validate.py .

## 9. 设计依据

- SDRF-Proteomics 官方规范：https://sdrf.quantms.org/specification.html
- TARGET Statement（2025）：https://jamanetwork.com/journals/jama/fullarticle/2837724

本草案的植物表结构、字段名及版本机制为项目设计，不是上述标准的原文或官方扩展。
