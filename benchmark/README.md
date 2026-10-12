# PlantCausal 首批公开数据 benchmark

两项均为本次新检索的公开研究数据，未使用之前的 ProtVis、SvH 或其他历史项目数据。

| ID | 公开来源 | 本次纳入范围 | 用途与状态 |
|---|---|---|---|
| PC_B001 | de Quesada 等，Tree Physiology 2024；Zenodo 10100580 | Measurements 的280个个体及指定终点 | 标准化/导入/校验；设计核查待完成 |
| PC_B002 | Hahn 等，Biogeosciences 2021；Zenodo 4306840 | 2014年生物量原表与日期转换 staging | 地块纵向数据导入；处理时间映射待完成 |

## PC_B001 映射与限制

来源：<https://zenodo.org/records/10100580>；论文：<https://doi.org/10.1093/treephys/tpae029>。原始数据作者包括 Gonzalo de Quesada、Jie Xu、Yann Salmon、Anna Lintunen、Sylvain Poque、Kristiina Himanen、Jussi Heinonsalo；准确作者与角色以 record.json 为准。许可 CC BY 4.0。

原始 code 保留为单位 ID。按论文一株一盆设计，单位类型设为 pot；此为显式一对一别名转换。treat D/M 保留为策略，strain 为先于干旱的暴露类别。只转换有明确时点的 trans_10_bio、trans_24_bio、wp_10d、wp_24d；不推断收获生物量的精确日期。缺失变为 NA/unknown，保留源文件、工作表、行号和列名。

论文的随机选样与干旱分配随机化须区分；emulation.design=observational 仅指未验证随机分配的分析路径，extensions.observed_design 明确原研究是受控实验。需核实托盘相关性与随机分配。蒸腾以干针叶生物量归一化，终点生物量不是可随意调整的基线混杂变量。

没有个体实际浇水、失访/死亡日志，不生成假的 treatments/events。时间序列工作簿只有组名、没有 Measurements 的个体 code，故保留原文件但不按行号跨表拼接。格式校验通过不等于因果分析就绪。

## PC_B002 映射与限制

来源：<https://zenodo.org/records/4306840>；论文：<https://doi.org/10.5194/bg-18-585-2021>。作者 Claudia Hahn、Andreas Lüscher、Sara Ernst-Hasler、Matthias Suter、Ansgar Kahmen。许可 CC BY 4.0。

原表有工作簿头部；第16行是字段名。staging 保留原 Plot、Cultivar、Treatment、Serie、Rep、Harvest、DW 与原行号；Excel 日期转换成真实 ISO 收获日期。完全没有品种信息的占位记录排除并计数；有效记录即使产量缺失仍保留。原表还包含豆科类别，不能无条件称所有记录都是论文的六种草。

尚未把所有季节处理日历与对照匹配关系整理为 protocol/units/treatments。不能用首个收获日充当处理时间零点，也不能把同一地块六次收获当六个独立重复。

## 可重复性

raw 保存未修改文件与仓库元数据；catalog 记录 DOI、许可、SHA-256、实际导入数与限制。Java 适配器固定源文件 SHA-256；测试逐项核对 PC_B001 的1120个数值终点/缺失单元。VALIDATION.md 记录软件验证结果。
