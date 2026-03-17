import { Layout, Menu, Tabs, Typography, Button, Space, Form, Input, Table, Upload, message, Spin, Descriptions, InputNumber, Row, Col, Card, Progress, Statistic } from 'antd';
import { UploadOutlined, PlayCircleOutlined, PlusOutlined } from '@ant-design/icons';
import { useEffect, useState } from 'react';
import axios from 'axios';
import './App.css';

const { Sider, Content } = Layout;
const { Title } = Typography;

interface Dataset {
  id: number;
  name: string;
  createdAt?: string;
}

interface TestCase {
  id: number;
  datasetId: number;
  question: string;
  referenceAnswer?: string;
  groundTruthChunkIds?: string[];
  createdAt?: string;
}

interface ChunkInfo {
  id: string;
  content: string;
  vectorSize: number;
}

interface EvaluationData {
  query: string;
  groundTruthDocs: string[];
  difficulty: string;
  category: string;
  source: string;
}

interface DatasetBuildResponse {
  data: EvaluationData[];
  totalCount: number;
  difficultyDistribution: Record<string, number>;
  categoryDistribution: Record<string, number>;
  sourceDistribution: Record<string, number>;
}

interface VectorChunkInfo {
  id: string;
  values: number[];
  metadata: Record<string, any>;
}

interface EvalJob {
  id: number;
  datasetId: number;
  status: string;
  progress: number;
  totalCases: number;
  createdAt?: string;
  finishedAt?: string;
  errorMessage?: string;
}

interface EvalResult {
  id: number;
  testCaseId: number;
  question: string;
  referenceAnswer?: string;
  modelAnswer?: string;
  answerRelevancyScore?: number;
  generatedQuestions?: string;
  perGenSimilarities?: string;
  createdAt?: string;
}

interface RagTestSource {
  id: string;
  title?: string;
  description?: string;
  text?: string;
  score?: number;
}

interface RagTestMetrics {
  prompt_tokens?: number;
  completion_tokens?: number;
  total_tokens?: number;
  outputTps?: number;
  duration?: number;
  model?: string;
  timestamp?: string;
}

interface RagTestResult {
  textResponse?: string | null;
  sources?: RagTestSource[];
  latencySeconds: number;
  metrics?: RagTestMetrics | null;
  error?: string | null;
}

const api = axios.create({
  baseURL: '/api',
});

function App() {
  const [datasets, setDatasets] = useState<Dataset[]>([]);
  const [loadingDatasets, setLoadingDatasets] = useState(false);
  const [selectedDatasetId, setSelectedDatasetId] = useState<number | null>(null);

  const [cases, setCases] = useState<TestCase[]>([]);
  const [loadingCases, setLoadingCases] = useState(false);

  const [evalJob, setEvalJob] = useState<EvalJob | null>(null);
  const [evalResults, setEvalResults] = useState<EvalResult[]>([]);
  const [polling, setPolling] = useState(false);

  const [importPreview, setImportPreview] = useState<any | null>(null);
  const [importLoading, setImportLoading] = useState(false);

  const [createDatasetForm] = Form.useForm();
  const [createCaseForm] = Form.useForm();
  const [evalForm] = Form.useForm();

  const [allChunks, setAllChunks] = useState<ChunkInfo[]>([]);
  const [loadingChunks, setLoadingChunks] = useState(false);
  const [selectedChunks, setSelectedChunks] = useState<string[]>([]);
  const [vectorData, setVectorData] = useState<VectorChunkInfo[]>([]);
  const [showVectorData, setShowVectorData] = useState(false);
  const [vectorJsonFilePath, setVectorJsonFilePath] = useState(() => {
    try {
      const saved = localStorage.getItem('vectorJsonFilePath');
      if (saved) return saved;
    } catch {}
    // 提供默认路径
    return '/Users/apple/Library/Application Support/anythingllm-desktop/storage/vector-cache/cc71b92b-4cf4-51f7-93ce-788cace0c96d.json';
  });

  const [evalConfig, setEvalConfig] = useState(() => {
    try {
      const saved = localStorage.getItem('evalConfig');
      if (saved) return JSON.parse(saved);
    } catch {}
    return {
      baseUrl: 'http://localhost:1234',
      embeddingBaseUrl: '',
      apiKey: '',
      embeddingApiKey: '',
      chatModel: 'gpt-4o-mini',
      embeddingModel: '',
      temperature: 0.2,
    };
  });

  const handleEvalConfigChange = (_: any, allValues: any) => {
    setEvalConfig(allValues);
    localStorage.setItem('evalConfig', JSON.stringify(allValues));
  };

  const [ragTestForm] = Form.useForm();
  const [ragTestLoading, setRagTestLoading] = useState(false);
  const [ragTestResult, setRagTestResult] = useState<RagTestResult | null>(null);

  const [ragConfig, setRagConfig] = useState(() => {
    try {
      const saved = localStorage.getItem('ragConfig');
      if (saved) return JSON.parse(saved);
    } catch {}
    return {
      baseUrl: 'http://localhost:3001/api/v1',
      apiKey: '',
      workspaceId: '57d56e93-4937-4f9f-8de5-621f99501f14',
      message: '介绍一下报销流程、薪酬发放、银行账户信息及发票开票信息等内容',
    };
  });

  const handleRagConfigChange = (_: any, allValues: any) => {
    setRagConfig(allValues);
    localStorage.setItem('ragConfig', JSON.stringify(allValues));
  };

  interface RagEvalMetrics {
    avg_correctness?: number;
    avg_faithfulness?: number;
    avg_relevance?: number;
    avg_coherence?: number;
    avg_response_time_ms?: number;
    overall_score?: number;
    total_test_cases?: number;
    successful_cases?: number;
  }

  interface RagEvalCaseResult {
    query: string;
    modelAnswer?: string;
    groundTruthAnswer?: string;
    correctness?: number;
    faithfulness?: number;
    relevance?: number;
    coherence?: number;
    responseTimeMs?: number;
    sourceCount?: number;
    error?: string;
  }

  interface RagEvalResult {
    testCases: RagEvalCaseResult[];
    metrics: RagEvalMetrics;
  }

  const [ragEvalForm] = Form.useForm();
  const [ragEvalLoading, setRagEvalLoading] = useState(false);
  const [ragEvalResult, setRagEvalResult] = useState<RagEvalResult | null>(null);

  const [ragEvalConfig, setRagEvalConfig] = useState(() => {
    try {
      const saved = localStorage.getItem('ragEvalConfig');
      if (saved) return JSON.parse(saved);
    } catch {}
    return {
      baseUrl: 'http://localhost:3001/api/v1',
      apiKey: '',
      workspaceId: '57d56e93-4937-4f9f-8de5-621f99501f14',
    };
  });

  const handleRagEvalConfigChange = (_: any, allValues: any) => {
    setRagEvalConfig(allValues);
    localStorage.setItem('ragEvalConfig', JSON.stringify(allValues));
  };

  // 数据构建模块状态
  const [datasetBuilderForm] = Form.useForm();
  const [buildingDataset, setBuildingDataset] = useState(false);
  const [buildResult, setBuildResult] = useState<DatasetBuildResponse | null>(null);
  const [buildDocuments, setBuildDocuments] = useState<string[]>([]);
  const [buildTargetSize, setBuildTargetSize] = useState(100);
  const [buildResultPage, setBuildResultPage] = useState(1);
  const [buildResultPageSize, setBuildResultPageSize] = useState(10);

  const handleBuildDataset = async (values: any) => {
    if (!buildDocuments || buildDocuments.length === 0) {
      message.warning('请先输入文档内容');
      return;
    }

    setBuildingDataset(true);
    setBuildResult(null);
    try {
      // 收集用户手动添加的测试用例及其参考答案
      const userTestCases = cases ? cases.map(c => ({
        question: c.question,
        referenceAnswer: c.referenceAnswer || ''
      })) : [];
      
      const res = await api.post<DatasetBuildResponse>('/dataset-builder/build', {
        documents: buildDocuments,
        targetSize: values.targetSize || 100,
        baseUrl: ragEvalConfig.baseUrl,
        apiKey: ragEvalConfig.apiKey,
        workspaceId: ragEvalConfig.workspaceId,
        userTestCases: userTestCases,  // 传递用户的测试用例及参考答案
      });
      setBuildResult(res.data);
      message.success(`成功构建 ${res.data.totalCount} 条评测数据`);
    } catch (e: any) {
      message.error('构建数据集失败: ' + (e.response?.data?.message || e.message));
    } finally {
      setBuildingDataset(false);
    }
  };

  const exportToExcel = (data: any[]) => {
    if (!data || data.length === 0) {
      message.warning('没有数据可导出');
      return;
    }

    try {
      // 动态导入xlsx库
      import('xlsx').then((XLSX) => {
        // 准备导出数据
        const exportData = data.map((item, index) => ({
          '序号': index + 1,
          '问题': item.query,
          'Ground Truth文档': item.groundTruthDocs ? item.groundTruthDocs.join('\n---\n') : '',
          '难度': item.difficulty,
          '类型': item.category,
          '来源': item.source,
        }));

        // 创建工作簿
        const ws = XLSX.utils.json_to_sheet(exportData);
        const wb = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(wb, ws, '评测数据集');

        // 设置列宽
        ws['!cols'] = [
          { wch: 8 },   // 序号
          { wch: 30 },  // 问题
          { wch: 50 },  // Ground Truth文档
          { wch: 10 },  // 难度
          { wch: 12 },  // 类型
          { wch: 12 },  // 来源
        ];

        // 导出文件
        const fileName = `RAG评测数据集_${new Date().getTime()}.xlsx`;
        XLSX.writeFile(wb, fileName);
        message.success('导出成功');
      }).catch(() => {
        message.error('导出失败，请确保已安装xlsx库');
      });
    } catch (error) {
      message.error('导出失败: ' + (error as any).message);
    }
  };

  const handleRagEval = async (values: any) => {
    if (!cases || cases.length === 0) {
      message.warning('请先在测试集中添加测试用例');
      return;
    }

    setRagEvalLoading(true);
    setRagEvalResult(null);
    try {
      const testCases = cases.map((c) => ({
        query: c.question,
        groundTruthAnswer: c.referenceAnswer || '',
        category: 'general',
        difficulty: 'medium',
      }));

      const res = await api.post<RagEvalResult>('/rag-eval/run', {
        baseUrl: values.baseUrl,
        apiKey: values.apiKey,
        workspaceId: values.workspaceId,
        testCases,
      });
      setRagEvalResult(res.data);
      message.success('RAG 评测完成');
    } catch (e: any) {
      message.error('RAG 评测失败: ' + (e.response?.data?.message || e.message));
    } finally {
      setRagEvalLoading(false);
    }
  };

  const handleRagTest = async (values: any) => {
    setRagTestLoading(true);
    setRagTestResult(null);
    try {
      const res = await api.post<RagTestResult>('/rag/test', {
        baseUrl: values.baseUrl,
        apiKey: values.apiKey,
        workspaceId: values.workspaceId,
        message: values.message,
      });
      setRagTestResult(res.data);
      if (res.data.error) {
        message.warning('RAG 返回错误: ' + res.data.error);
      } else {
        message.success('RAG 请求成功');
      }
    } catch (e: any) {
      message.error('请求失败: ' + (e.response?.data?.message || e.message));
      setRagTestResult({
        textResponse: null,
        sources: [],
        latencySeconds: 0,
        metrics: null,
        error: e.response?.data?.message || e.message,
      });
    } finally {
      setRagTestLoading(false);
    }
  };

  const fetchDatasets = async () => {
    setLoadingDatasets(true);
    try {
      const res = await api.get<Dataset[]>('/datasets');
      setDatasets(res.data);
      if (!selectedDatasetId && res.data.length > 0) {
        setSelectedDatasetId(res.data[0].id);
      }
    } catch (e) {
      message.error('加载测试集失败');
    } finally {
      setLoadingDatasets(false);
    }
  };

  useEffect(() => {
    fetchDatasets();
  }, []);

  useEffect(() => {
    const loadCases = async () => {
      if (!selectedDatasetId) {
        setCases([]);
        return;
      }
      setLoadingCases(true);
      try {
        const res = await api.get('/datasets/' + selectedDatasetId + '/cases', {
          params: { page: 0, size: 500 },
        });
        setCases(res.data.content ?? res.data);
      } catch {
        message.error('加载用例失败');
      } finally {
        setLoadingCases(false);
      }
    };
    loadCases();
  }, [selectedDatasetId]);

  const handleCreateDataset = async (values: any) => {
    try {
      const res = await api.post<Dataset>('/datasets', { name: values.name });
      message.success('创建测试集成功');
      createDatasetForm.resetFields();
      await fetchDatasets();
      setSelectedDatasetId(res.data.id);
    } catch {
      message.error('创建测试集失败');
    }
  };

  const handleDeleteCase = async (caseId: number) => {
    try {
      await api.delete(`/cases/${caseId}`);
      message.success('删除用例成功');
      setCases((prev) => prev.filter((c) => c.id !== caseId));
    } catch {
      message.error('删除用例失败');
    }
  };

  const handleCreateCase = async (values: any) => {
    if (!selectedDatasetId) {
      message.warning('请先选择一个测试集');
      return;
    }
    try {
      const res = await api.post<TestCase>(
        `/datasets/${selectedDatasetId}/cases`,
        {
          question: values.question,
          referenceAnswer: values.referenceAnswer || null,
          groundTruthChunkIds: selectedChunks.length > 0 ? selectedChunks : null,
        },
      );
      message.success('新增用例成功');
      createCaseForm.resetFields();
      setSelectedChunks([]);
      setCases((prev) => [res.data, ...prev]);
    } catch (e: any) {
      message.error('新增用例失败: ' + (e.response?.data?.message || e.message));
    }
  };

  const loadChunks = async () => {
    if (!ragConfig.workspaceId) {
      message.warning('请先配置 Workspace ID');
      return;
    }
    setLoadingChunks(true);
    try {
      const res = await api.post<{ chunks: ChunkInfo[]; count: number }>('/rag-chunks/list', {
        workspaceId: ragConfig.workspaceId,
      });
      setAllChunks(res.data.chunks || []);
      message.success(`加载了 ${res.data.count} 个 chunk`);
    } catch (e: any) {
      message.error('加载 chunk 失败: ' + (e.response?.data?.message || e.message));
    } finally {
      setLoadingChunks(false);
    }
  };

  const loadVectorData = async () => {
    if (!vectorJsonFilePath || vectorJsonFilePath.trim() === '') {
      message.warning('请先输入向量数据JSON文件路径');
      return;
    }
    setLoadingChunks(true);
    try {
      // 使用POST方式传递路径，避免URL编码问题
      const res = await api.post<{ data: VectorChunkInfo[][]; count: number }>('/rag-chunks/vector-data', {
        jsonFilePath: vectorJsonFilePath.trim(),
      });
      // 展平嵌套数组
      const flatData = res.data.data?.flat() || [];
      setVectorData(flatData);
      
      // 从向量数据中提取chunk信息，转换为ChunkInfo格式
      const chunks: ChunkInfo[] = flatData.map((item) => {
        // 从metadata中提取text字段
        let text = item.metadata?.text || '';
        
        // 移除<document_metadata>标签部分
        if (text.includes('</document_metadata>')) {
          const parts = text.split('</document_metadata>');
          text = parts.length > 1 ? parts[1].trim() : text;
        }
        
        // 如果text为空，使用description或title
        if (!text || text.length === 0) {
          text = item.metadata?.description || item.metadata?.title || '无内容';
        }
        
        return {
          id: item.id,
          content: text,
          vectorSize: item.values?.length || 0,
        };
      });
      
      setAllChunks(chunks);
      
      setShowVectorData(true);
      localStorage.setItem('vectorJsonFilePath', vectorJsonFilePath);
      message.success(`加载了 ${res.data.count} 个向量数据`);
    } catch (e: any) {
      message.error('加载向量数据失败: ' + (e.response?.data?.message || e.message));
      console.error('详细错误:', e.response?.data);
    } finally {
      setLoadingChunks(false);
    }
  };

  const uploadProps = {
    name: 'file',
    accept: '.xlsx',
    showUploadList: false,
    customRequest: async (options: any) => {
      if (!selectedDatasetId) {
        message.warning('请先选择一个测试集');
        return;
      }
      const { file, onSuccess, onError } = options;
      const formData = new FormData();
      formData.append('file', file as File);
      setImportLoading(true);
      try {
        const res = await api.post(`/datasets/${selectedDatasetId}/imports/excel`, formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        });
        setImportPreview(res.data);
        message.success('解析成功，请检查预览');
        onSuccess(res.data);
      } catch (err) {
        message.error('解析 Excel 失败');
        onError?.(err);
      } finally {
        setImportLoading(false);
      }
    },
  };

  const handleCommitImport = async () => {
    if (!selectedDatasetId || !importPreview?.token) {
      message.warning('没有可导入的数据');
      return;
    }
    setImportLoading(true);
    try {
      await api.post(`/datasets/${selectedDatasetId}/imports/${importPreview.token}/commit`);
      message.success('导入成功');
      setImportPreview(null);
      // reload cases
      const res = await api.get('/datasets/' + selectedDatasetId + '/cases', {
        params: { page: 0, size: 500 },
      });
      setCases(res.data.content ?? res.data);
    } catch {
      message.error('导入失败');
    } finally {
      setImportLoading(false);
    }
  };

  const handleStartEval = async (values: any) => {
    if (!selectedDatasetId) {
      message.warning('请先选择一个测试集');
      return;
    }
    try {
      const res = await api.post<EvalJob>('/eval-jobs', {
        datasetId: selectedDatasetId,
        baseUrl: values.baseUrl,
        embeddingBaseUrl: values.embeddingBaseUrl,
        apiKey: values.apiKey,
        embeddingApiKey: values.embeddingApiKey,
        chatModel: values.chatModel,
        embeddingModel: values.embeddingModel,
        temperature: values.temperature,
      });
      setEvalJob(res.data);
      setEvalResults([]);
      message.success('测评任务已创建，开始执行');
      startPolling(res.data.id);
    } catch {
      message.error('创建测评任务失败');
    }
  };

  const startPolling = (jobId: number) => {
    setPolling(true);
    const interval = setInterval(async () => {
      try {
        const jobRes = await api.get<EvalJob>(`/eval-jobs/${jobId}`);
        setEvalJob(jobRes.data);
        if (jobRes.data.status === 'SUCCEEDED' || jobRes.data.status === 'FAILED') {
          const resRes = await api.get(`/eval-jobs/${jobId}/results`, {
            params: { page: 0, size: 500 },
          });
          setEvalResults(resRes.data.content ?? resRes.data);
          clearInterval(interval);
          setPolling(false);
          message.info('测评任务已完成');
        }
      } catch {
        clearInterval(interval);
        setPolling(false);
        message.error('轮询任务状态失败');
      }
    }, 3000);
  };

  const datasetMenuItems = datasets.map((d) => ({
    key: d.id.toString(),
    label: d.name,
  }));

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={240} theme="light">
        <div style={{ padding: 16, borderBottom: '1px solid #f0f0f0' }}>
          <Title level={4} style={{ marginBottom: 12 }}>
            测试集管理
          </Title>
          <Form layout="vertical" form={createDatasetForm} onFinish={handleCreateDataset}>
            <Form.Item name="name" label="新建测试集" rules={[{ required: true, message: '请输入名称' }]}>
              <Input placeholder="如：客服问答测试集" />
            </Form.Item>
            <Form.Item>
              <Button type="primary" size="small" htmlType="submit" icon={<PlusOutlined />} block>
                新建
              </Button>
            </Form.Item>
          </Form>
        </div>
        <Spin spinning={loadingDatasets}>
          <Menu
            mode="inline"
            selectedKeys={selectedDatasetId ? [selectedDatasetId.toString()] : []}
            items={datasetMenuItems}
            onClick={(info) => setSelectedDatasetId(Number(info.key))}
            style={{ borderRight: 0 }}
          />
        </Spin>
      </Sider>
      <Layout>
        <Content style={{ padding: 24, height: '100%', overflow: 'auto' }}>
          <Title level={3} style={{ marginBottom: 16 }}>
            RAG测评系统
          </Title>
          <Tabs
            defaultActiveKey="cases"
            items={[
              {
                key: 'cases',
                label: '编辑测试集',
                children: (
                  <>
                    <Space direction="vertical" size="large" style={{ width: '100%', marginBottom: 24 }}>
                      <div>
                        <div style={{ marginBottom: 12 }}>
                          <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>
                            向量数据JSON文件路径
                          </label>
                          <Space style={{ width: '100%' }}>
                            <Input
                              placeholder="输入JSON文件完整路径，例如: /Users/apple/Library/Application Support/anythingllm-desktop/storage/vector-cache/cc71b92b-4cf4-51f7-93ce-788cace0c96d.json"
                              value={vectorJsonFilePath}
                              onChange={(e) => setVectorJsonFilePath(e.target.value)}
                              style={{ flex: 1 }}
                            />
                            <Button onClick={loadVectorData} loading={loadingChunks} type="primary">
                              加载向量数据
                            </Button>
                          </Space>
                        </div>
                        {showVectorData && vectorData.length > 0 && (
                          <div style={{ marginBottom: 12, padding: 12, background: '#f5f5f5', borderRadius: 4 }}>
                            <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                              <strong>向量数据 ({vectorData.length} 条)</strong>
                              <Button size="small" onClick={() => setShowVectorData(false)}>
                                隐藏
                              </Button>
                            </div>
                            <div
                              style={{
                                border: '1px solid #d9d9d9',
                                borderRadius: 4,
                                maxHeight: 400,
                                overflow: 'auto',
                              }}
                            >
                              <Table<VectorChunkInfo>
                                rowKey="id"
                                size="small"
                                dataSource={vectorData}
                                pagination={{ pageSize: 5 }}
                                columns={[
                                  {
                                    title: '向量ID',
                                    dataIndex: 'id',
                                    width: 120,
                                    ellipsis: true,
                                    render: (text: string) => (
                                      <span title={text} style={{ fontSize: 12 }}>
                                        {text.substring(0, 16)}...
                                      </span>
                                    ),
                                  },
                                  {
                                    title: '维度',
                                    dataIndex: 'values',
                                    width: 60,
                                    render: (values: number[]) => values?.length || 0,
                                  },
                                  {
                                    title: '文档标题',
                                    width: 120,
                                    render: (_, record) => (
                                      <span title={record.metadata?.title} style={{ fontSize: 12 }}>
                                        {record.metadata?.title ? record.metadata.title.substring(0, 30) : '-'}
                                      </span>
                                    ),
                                  },
                                  {
                                    title: '文本内容预览',
                                    width: 300,
                                    render: (_, record) => {
                                      const text = record.metadata?.text;
                                      if (!text) return <span style={{ color: '#999' }}>-</span>;
                                      return (
                                        <span
                                          style={{
                                            fontSize: 12,
                                            color: '#333',
                                            display: 'block',
                                            overflow: 'hidden',
                                            textOverflow: 'ellipsis',
                                            whiteSpace: 'nowrap',
                                          }}
                                          title={text}
                                        >
                                          {text.substring(0, 100)}...
                                        </span>
                                      );
                                    },
                                  },
                                ]}
                                expandable={{
                                  expandedRowRender: (record) => (
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                                      {record.metadata?.text && (
                                        <div>
                                          <strong style={{ fontSize: 14, marginBottom: 8, display: 'block' }}>
                                            📄 文本内容
                                          </strong>
                                          <div
                                            style={{
                                              background: '#fafafa',
                                              padding: 12,
                                              borderRadius: 4,
                                              fontSize: 13,
                                              lineHeight: 1.6,
                                              whiteSpace: 'pre-wrap',
                                              wordBreak: 'break-word',
                                              maxHeight: 400,
                                              overflow: 'auto',
                                              border: '1px solid #e8e8e8',
                                            }}
                                          >
                                            {record.metadata.text}
                                          </div>
                                        </div>
                                      )}
                                      <div>
                                        <strong style={{ fontSize: 14, marginBottom: 8, display: 'block' }}>
                                          🏷️ 元数据
                                        </strong>
                                        <pre
                                          style={{
                                            background: '#fafafa',
                                            padding: 12,
                                            borderRadius: 4,
                                            fontSize: 12,
                                            maxHeight: 300,
                                            overflow: 'auto',
                                            border: '1px solid #e8e8e8',
                                          }}
                                        >
                                          {JSON.stringify(record.metadata, null, 2)}
                                        </pre>
                                      </div>
                                      <div>
                                        <strong style={{ fontSize: 14, marginBottom: 8, display: 'block' }}>
                                          📊 向量值（前20维）
                                        </strong>
                                        <pre
                                          style={{
                                            background: '#fafafa',
                                            padding: 12,
                                            borderRadius: 4,
                                            fontSize: 12,
                                            maxHeight: 200,
                                            overflow: 'auto',
                                            border: '1px solid #e8e8e8',
                                          }}
                                        >
                                          {JSON.stringify(record.values?.slice(0, 20), null, 2)}
                                        </pre>
                                      </div>
                                    </div>
                                  ),
                                }}
                              />
                            </div>
                          </div>
                        )}
                      </div>
                    </Space>
                    <Form
                      layout="vertical"
                      form={createCaseForm}
                      onFinish={handleCreateCase}
                      style={{ marginBottom: 24 }}
                    >
                      <Row gutter={16}>
                        <Col span={8}>
                          <Form.Item
                            name="question"
                            label="问题"
                            rules={[{ required: true, message: '请输入问题' }]}
                          >
                            <Input.TextArea rows={3} placeholder="输入问题内容" />
                          </Form.Item>
                        </Col>
                        <Col span={8}>
                          <Form.Item name="referenceAnswer" label="参考答案">
                            <Input.TextArea rows={3} placeholder="输入参考答案（可选）" />
                          </Form.Item>
                        </Col>
                        <Col span={8}>
                          <Form.Item label="Ground Truth Chunk（可选）">
                            <div
                              style={{
                                border: '1px solid #d9d9d9',
                                borderRadius: 4,
                                padding: 8,
                                maxHeight: 200,
                                overflow: 'auto',
                                background: '#fafafa',
                              }}
                            >
                              {allChunks.length === 0 ? (
                                <div style={{ color: '#999', fontSize: 12, padding: 8 }}>
                                  请先加载向量数据
                                </div>
                              ) : (
                                allChunks.map((chunk) => {
                                  // chunk.content已经是从metadata.text提取的内容
                                  const displayText = chunk.content || '无内容';
                                  
                                  // 检查是否是文件名（包含.pdf等扩展名）
                                  const isFileName = /\.(pdf|doc|docx|txt|xlsx|pptx)$/i.test(displayText);
                                  
                                  // 如果是文件名，显示"[文件]"标记
                                  let summary = displayText;
                                  if (isFileName) {
                                    summary = `[文件] ${displayText}`;
                                  } else if (displayText.length > 60) {
                                    summary = displayText.substring(0, 60) + '...';
                                  }
                                  
                                  return (
                                    <div 
                                      key={chunk.id} 
                                      style={{ 
                                        marginBottom: 6, 
                                        display: 'flex', 
                                        alignItems: 'flex-start',
                                        padding: 6,
                                        borderRadius: 3,
                                        backgroundColor: selectedChunks.includes(chunk.id) ? '#e6f7ff' : 'transparent',
                                        cursor: 'pointer',
                                        border: selectedChunks.includes(chunk.id) ? '1px solid #1890ff' : '1px solid transparent',
                                        transition: 'all 0.2s',
                                      }}
                                      onClick={() => {
                                        if (selectedChunks.includes(chunk.id)) {
                                          setSelectedChunks(selectedChunks.filter((id) => id !== chunk.id));
                                        } else {
                                          setSelectedChunks([...selectedChunks, chunk.id]);
                                        }
                                      }}
                                      title={displayText}
                                    >
                                      <input
                                        type="checkbox"
                                        checked={selectedChunks.includes(chunk.id)}
                                        onChange={(e) => {
                                          e.stopPropagation();
                                          if (e.target.checked) {
                                            setSelectedChunks([...selectedChunks, chunk.id]);
                                          } else {
                                            setSelectedChunks(selectedChunks.filter((id) => id !== chunk.id));
                                          }
                                        }}
                                        style={{ marginTop: 3, marginRight: 6, flexShrink: 0, cursor: 'pointer' }}
                                      />
                                      <span
                                        style={{
                                          fontSize: 12,
                                          color: isFileName ? '#ff7875' : '#333',
                                          wordBreak: 'break-word',
                                          flex: 1,
                                          lineHeight: 1.4,
                                          whiteSpace: 'normal',
                                          fontWeight: isFileName ? 'bold' : 'normal',
                                        }}
                                      >
                                        {summary}
                                      </span>
                                    </div>
                                  );
                                })
                              )}
                            </div>
                            {selectedChunks.length > 0 && (
                              <div style={{ marginTop: 8, fontSize: 12, color: '#1890ff' }}>
                                已选择 {selectedChunks.length} 个 chunk
                              </div>
                            )}
                          </Form.Item>
                        </Col>
                      </Row>
                      <Form.Item>
                        <Button type="primary" htmlType="submit" icon={<PlusOutlined />} size="large">
                          新增用例
                        </Button>
                      </Form.Item>
                    </Form>
                    <Spin spinning={loadingCases}>
                      <Table<TestCase>
                        rowKey="id"
                        size="small"
                        dataSource={cases}
                        pagination={{ pageSize: 10 }}
                        columns={[
                          { title: 'ID', dataIndex: 'id', width: 60 },
                          { title: '问题', dataIndex: 'question' },
                          { title: '参考答案', dataIndex: 'referenceAnswer' },
                          {
                            title: '依据',
                            dataIndex: 'groundTruthChunkIds',
                            width: 100,
                            render: (ids: string[] | undefined) =>
                              ids && ids.length > 0 ? (
                                <span style={{ color: '#1890ff' }}>{ids.length} 个 chunk</span>
                              ) : (
                                <span style={{ color: '#999' }}>-</span>
                              ),
                          },
                          {
                            title: '操作',
                            width: 80,
                            render: (_, record) => (
                              <Button
                                type="text"
                                danger
                                size="small"
                                onClick={() => {
                                  if (window.confirm('确定要删除这个用例吗？')) {
                                    handleDeleteCase(record.id);
                                  }
                                }}
                              >
                                删除
                              </Button>
                            ),
                          },
                        ]}
                        expandable={{
                          expandedRowRender: (record) => (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                              <div>
                                <strong style={{ fontSize: 14, marginBottom: 8, display: 'block' }}>
                                  ❓ 问题
                                </strong>
                                <div
                                  style={{
                                    background: '#fafafa',
                                    padding: 12,
                                    borderRadius: 4,
                                    fontSize: 13,
                                    lineHeight: 1.6,
                                    whiteSpace: 'pre-wrap',
                                    wordBreak: 'break-word',
                                    border: '1px solid #e8e8e8',
                                  }}
                                >
                                  {record.question}
                                </div>
                              </div>
                              <div>
                                <strong style={{ fontSize: 14, marginBottom: 8, display: 'block' }}>
                                  ✅ 参考答案
                                </strong>
                                <div
                                  style={{
                                    background: '#fafafa',
                                    padding: 12,
                                    borderRadius: 4,
                                    fontSize: 13,
                                    lineHeight: 1.6,
                                    whiteSpace: 'pre-wrap',
                                    wordBreak: 'break-word',
                                    border: '1px solid #e8e8e8',
                                  }}
                                >
                                  {record.referenceAnswer || '-'}
                                </div>
                              </div>
                              {record.groundTruthChunkIds && record.groundTruthChunkIds.length > 0 && (
                                <div>
                                  <strong style={{ fontSize: 14, marginBottom: 8, display: 'block' }}>
                                    📚 Ground Truth Chunks ({record.groundTruthChunkIds.length})
                                  </strong>
                                  <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                                    {record.groundTruthChunkIds.map((chunkId, idx) => {
                                      // 从allChunks中查找对应的chunk
                                      const chunk = allChunks.find((c) => c.id === chunkId);
                                      return (
                                        <div
                                          key={idx}
                                          style={{
                                            background: '#f0f5ff',
                                            padding: 12,
                                            borderRadius: 4,
                                            border: '1px solid #b3d8ff',
                                          }}
                                        >
                                          <div style={{ marginBottom: 8 }}>
                                            <strong style={{ color: '#1890ff', fontSize: 12 }}>
                                              Chunk ID:
                                            </strong>
                                            <div
                                              style={{
                                                background: '#fff',
                                                padding: 6,
                                                borderRadius: 2,
                                                fontSize: 12,
                                                wordBreak: 'break-all',
                                                marginTop: 4,
                                                border: '1px solid #d9d9d9',
                                              }}
                                            >
                                              {chunkId}
                                            </div>
                                          </div>
                                          <div>
                                            <strong style={{ color: '#1890ff', fontSize: 12 }}>
                                              文本内容:
                                            </strong>
                                            <div
                                              style={{
                                                background: '#fff',
                                                padding: 8,
                                                borderRadius: 2,
                                                fontSize: 12,
                                                lineHeight: 1.6,
                                                whiteSpace: 'pre-wrap',
                                                wordBreak: 'break-word',
                                                marginTop: 4,
                                                maxHeight: 200,
                                                overflow: 'auto',
                                                border: '1px solid #d9d9d9',
                                              }}
                                            >
                                              {chunk?.content || '内容未找到'}
                                            </div>
                                          </div>
                                        </div>
                                      );
                                    })}
                                  </div>
                                </div>
                              )}
                            </div>
                          ),
                        }}
                      />
                    </Spin>
                  </>
                ),
              },
              {
                key: 'import',
                label: 'Excel 导入',
                children: (
                  <>
                    <Space direction="vertical" size="large" style={{ width: '100%' }}>
                      <Upload {...uploadProps}>
                        <Button icon={<UploadOutlined />}>上传 Excel (.xlsx)</Button>
                      </Upload>
                      <Spin spinning={importLoading}>
                        {importPreview && (
                          <>
                            <div>
                              有效行：{importPreview.validCount}，错误行：
                              {importPreview.errorCount}
                            </div>
                            <Table
                              rowKey="rowNumber"
                              size="small"
                              dataSource={importPreview.rows}
                              pagination={{ pageSize: 10 }}
                              columns={[
                                { title: '行号', dataIndex: 'rowNumber', width: 80 },
                                { title: '问题', dataIndex: 'question' },
                                { title: '参考答案', dataIndex: 'referenceAnswer' },
                                { title: '错误', dataIndex: 'error' },
                              ]}
                            />
                            <Button type="primary" onClick={handleCommitImport} disabled={!importPreview?.validCount}>
                              确认导入
                            </Button>
                          </>
                        )}
                      </Spin>
                    </Space>
                  </>
                ),
              },
              {
                key: 'eval',
                label: '问答相关性测试',
                children: (
                  <div
                    style={{
                      display: 'flex',
                      flexDirection: 'column',
                      gap: 24,
                      width: '100%',
                    }}
                  >
                    <Row gutter={16} align="top">
                      <Col span={14}>
                        <Form
                          layout="vertical"
                          form={evalForm}
                          onFinish={handleStartEval}
                          initialValues={evalConfig}
                          onValuesChange={handleEvalConfigChange}
                        >
                          <Row gutter={16}>
                            <Col span={12}>
                              <Form.Item
                                name="baseUrl"
                                label="大模型 Base URL"
                                rules={[{ required: true, message: '请输入 Base URL' }]}
                              >
                                <Input placeholder="如：http://localhost:8000" />
                              </Form.Item>
                            </Col>
                            <Col span={12}>
                              <Form.Item
                                name="embeddingBaseUrl"
                                label="Embedding Base URL（可选）"
                                tooltip="不填则默认使用上面的 Base URL"
                              >
                                <Input placeholder="如：https://dashscope.aliyuncs.com/compatible-mode/v1" />
                              </Form.Item>
                            </Col>
                          </Row>
                          <Row gutter={16}>
                            <Col span={12}>
                              <Form.Item
                                name="apiKey"
                                label="API Key"
                                rules={[{ required: true, message: '请输入 API Key' }]}
                              >
                                <Input.Password />
                              </Form.Item>
                            </Col>
                            <Col span={12}>
                              <Form.Item
                                name="embeddingApiKey"
                                label="Embedding API Key（可选）"
                                tooltip="不填则默认使用上面的 API Key"
                              >
                                <Input.Password />
                              </Form.Item>
                            </Col>
                          </Row>
                          <Row gutter={16}>
                            <Col span={12}>
                              <Form.Item
                                name="chatModel"
                                label="对话模型名称"
                                rules={[{ required: true, message: '请输入对话模型名称' }]}
                              >
                                <Input placeholder="如：gpt-4o-mini" />
                              </Form.Item>
                            </Col>
                            <Col span={12}>
                              <Form.Item
                                name="embeddingModel"
                                label="Embedding 模型名称（可选）"
                                tooltip="留空则默认使用对话模型名称"
                              >
                                <Input placeholder="如：text-embedding-3-small" />
                              </Form.Item>
                            </Col>
                          </Row>
                          <Row gutter={16}>
                            <Col span={6}>
                              <Form.Item name="temperature" label="温度">
                                <InputNumber min={0} max={1} step={0.1} style={{ width: '100%' }} />
                              </Form.Item>
                            </Col>
                            <Col span={6} style={{ display: 'flex', alignItems: 'flex-end' }}>
                              <Form.Item style={{ marginBottom: 0 }}>
                                <Button
                                  type="primary"
                                  htmlType="submit"
                                  icon={<PlayCircleOutlined />}
                                  disabled={!selectedDatasetId || polling}
                                  block
                                >
                                  启动测评
                                </Button>
                              </Form.Item>
                            </Col>
                          </Row>
                        </Form>
                      </Col>
                      <Col span={10}>
                        {evalJob && (
                          <Descriptions
                            title="当前测评任务"
                            bordered
                            size="small"
                            column={2}
                            style={{ width: '100%' }}
                          >
                            <Descriptions.Item label="任务 ID">{evalJob.id}</Descriptions.Item>
                            <Descriptions.Item label="数据集 ID">
                              {evalJob.datasetId}
                            </Descriptions.Item>
                            <Descriptions.Item label="状态">{evalJob.status}</Descriptions.Item>
                            <Descriptions.Item label="进度">
                              {evalJob.progress}/{evalJob.totalCases}
                            </Descriptions.Item>
                            <Descriptions.Item label="创建时间" span={2}>
                              {evalJob.createdAt}
                            </Descriptions.Item>
                            <Descriptions.Item label="完成时间" span={2}>
                              {evalJob.finishedAt || '-'}
                            </Descriptions.Item>
                            {evalJob.errorMessage && (
                              <Descriptions.Item label="错误" span={2}>
                                {evalJob.errorMessage}
                              </Descriptions.Item>
                            )}
                          </Descriptions>
                        )}
                      </Col>
                    </Row>

                    <Spin spinning={polling}>
                      <Table<EvalResult>
                        rowKey="id"
                        size="small"
                        dataSource={evalResults}
                        pagination={{ pageSize: 5 }}
                        style={{ width: '100%' }}
                        expandable={{
                          expandedRowRender: (record) => (
                            <div>
                              <p>
                                <strong>模型答案：</strong>
                              </p>
                              <p style={{ whiteSpace: 'pre-wrap' }}>
                                {record.modelAnswer || '-'}
                              </p>
                              <p>
                                <strong>生成问题列表：</strong>
                              </p>
                              <pre
                                style={{
                                  background: '#fafafa',
                                  padding: 8,
                                  whiteSpace: 'pre-wrap',
                                }}
                              >
                                {record.generatedQuestions}
                              </pre>
                              <p>
                                <strong>每次相似度：</strong>
                              </p>
                              <pre
                                style={{
                                  background: '#fafafa',
                                  padding: 8,
                                  whiteSpace: 'pre-wrap',
                                }}
                              >
                                {record.perGenSimilarities}
                              </pre>
                            </div>
                          ),
                        }}
                        columns={[
                          { title: '用例 ID', dataIndex: 'testCaseId', width: 80 },
                          { title: '问题', dataIndex: 'question' },
                          { title: '参考答案', dataIndex: 'referenceAnswer' },
                          {
                            title: '答案相关性得分',
                            dataIndex: 'answerRelevancyScore',
                            width: 160,
                            render: (v: number | undefined) =>
                              v !== undefined ? v.toFixed(3) : '-',
                          },
                        ]}
                      />
                    </Spin>
                  </div>
                ),
              },
              {
                key: 'rag',
                label: 'RAG 接口测试',
                children: (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 24, width: '100%' }}>
                    <Form
                      layout="vertical"
                      form={ragTestForm}
                      onFinish={handleRagTest}
                      initialValues={ragConfig}
                      onValuesChange={handleRagConfigChange}
                    >
                      <Row gutter={16}>
                        <Col span={12}>
                          <Form.Item
                            name="baseUrl"
                            label="Base URL"
                            rules={[{ required: true, message: '请输入 Base URL' }]}
                          >
                            <Input placeholder="http://localhost:3001/api/v1" />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item
                            name="apiKey"
                            label="API Key"
                            rules={[{ required: true, message: '请输入 API Key' }]}
                          >
                            <Input.Password placeholder="AnyLLM API Key" />
                          </Form.Item>
                        </Col>
                      </Row>
                      <Row gutter={16}>
                        <Col span={12}>
                          <Form.Item
                            name="workspaceId"
                            label="Workspace ID"
                            rules={[{ required: true, message: '请输入 Workspace ID' }]}
                          >
                            <Input placeholder="57d56e93-4937-4f9f-8de5-621f99501f14" />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item
                            name="message"
                            label="提问内容"
                            rules={[{ required: true, message: '请输入提问内容' }]}
                          >
                            <Input.TextArea rows={2} placeholder="输入要问 RAG 的问题" />
                          </Form.Item>
                        </Col>
                      </Row>
                      <Form.Item>
                        <Button
                          type="primary"
                          htmlType="submit"
                          loading={ragTestLoading}
                          icon={<PlayCircleOutlined />}
                        >
                          测试 RAG
                        </Button>
                      </Form.Item>
                    </Form>

                    <Spin spinning={ragTestLoading}>
                      {ragTestResult && (
                        <>
                          {ragTestResult.error && (
                            <Descriptions title="错误" bordered size="small" column={1}>
                              <Descriptions.Item label="消息">
                                {ragTestResult.error}
                              </Descriptions.Item>
                            </Descriptions>
                          )}
                          {ragTestResult.latencySeconds > 0 && (
                            <Descriptions title="耗时" bordered size="small" column={2}>
                              <Descriptions.Item label="客户端耗时">
                                {ragTestResult.latencySeconds.toFixed(2)} s
                              </Descriptions.Item>
                              {ragTestResult.metrics?.duration != null && (
                                <Descriptions.Item label="服务端 duration">
                                  {(ragTestResult.metrics.duration / 1000).toFixed(2)} s
                                </Descriptions.Item>
                              )}
                            </Descriptions>
                          )}
                          {ragTestResult.metrics && (
                            <Descriptions title="模型用量" bordered size="small" column={2}>
                              <Descriptions.Item label="prompt_tokens">
                                {ragTestResult.metrics.prompt_tokens ?? '-'}
                              </Descriptions.Item>
                              <Descriptions.Item label="completion_tokens">
                                {ragTestResult.metrics.completion_tokens ?? '-'}
                              </Descriptions.Item>
                              <Descriptions.Item label="total_tokens">
                                {ragTestResult.metrics.total_tokens ?? '-'}
                              </Descriptions.Item>
                              <Descriptions.Item label="model">
                                {ragTestResult.metrics.model ?? '-'}
                              </Descriptions.Item>
                            </Descriptions>
                          )}
                          {ragTestResult.textResponse != null && (
                            <div>
                              <Title level={5}>RAG 回答</Title>
                              <pre
                                style={{
                                  background: '#fafafa',
                                  padding: 16,
                                  whiteSpace: 'pre-wrap',
                                  maxHeight: 400,
                                  overflow: 'auto',
                                }}
                              >
                                {ragTestResult.textResponse}
                              </pre>
                            </div>
                          )}
                          {ragTestResult.sources && ragTestResult.sources.length > 0 && (
                            <div>
                              <Title level={5}>引用来源 ({ragTestResult.sources.length})</Title>
                              <Table
                                rowKey="id"
                                size="small"
                                dataSource={ragTestResult.sources}
                                pagination={{ pageSize: 5 }}
                                columns={[
                                  { 
                                    title: '文档ID', 
                                    dataIndex: 'id', 
                                    width: 150,
                                    ellipsis: true,
                                    render: (id: string) => (
                                      <span title={id} style={{ fontSize: 12, color: '#666' }}>
                                        {id}
                                      </span>
                                    ),
                                  },
                                  { title: '标题', dataIndex: 'title', width: 150, ellipsis: true },
                                  {
                                    title: 'score',
                                    dataIndex: 'score',
                                    width: 80,
                                    render: (v: number) => (v != null ? v.toFixed(4) : '-'),
                                  },
                                  {
                                    title: '摘要',
                                    dataIndex: 'text',
                                    ellipsis: true,
                                    render: (t: string) =>
                                      t != null ? (t.length > 150 ? t.slice(0, 150) + '...' : t) : '-',
                                  },
                                ]}
                                expandable={{
                                  expandedRowRender: (record) => (
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                                      <div>
                                        <strong>文档ID：</strong>
                                        <div style={{ 
                                          background: '#fafafa', 
                                          padding: 8, 
                                          borderRadius: 4,
                                          wordBreak: 'break-all',
                                          fontSize: 12,
                                          marginTop: 4,
                                        }}>
                                          {record.id ?? '-'}
                                        </div>
                                      </div>
                                      <div>
                                        <strong>完整文本：</strong>
                                        <pre
                                          style={{
                                            background: '#fafafa',
                                            padding: 8,
                                            whiteSpace: 'pre-wrap',
                                            fontSize: 12,
                                            maxHeight: 300,
                                            overflow: 'auto',
                                            borderRadius: 4,
                                            marginTop: 4,
                                          }}
                                        >
                                          {record.text ?? '-'}
                                        </pre>
                                      </div>
                                    </div>
                                  ),
                                }}
                              />
                            </div>
                          )}
                        </>
                      )}
                    </Spin>
                  </div>
                ),
              },
              {
                key: 'rag-eval',
                label: 'RAG 评测',
                children: (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 24, width: '100%' }}>
                    <Form
                      layout="vertical"
                      form={ragEvalForm}
                      onFinish={handleRagEval}
                      initialValues={ragEvalConfig}
                      onValuesChange={handleRagEvalConfigChange}
                    >
                      <Row gutter={16}>
                        <Col span={8}>
                          <Form.Item
                            name="baseUrl"
                            label="Base URL"
                            rules={[{ required: true, message: '请输入 Base URL' }]}
                          >
                            <Input placeholder="http://localhost:3001/api/v1" />
                          </Form.Item>
                        </Col>
                        <Col span={8}>
                          <Form.Item
                            name="apiKey"
                            label="API Key"
                            rules={[{ required: true, message: '请输入 API Key' }]}
                          >
                            <Input.Password placeholder="AnyLLM API Key" />
                          </Form.Item>
                        </Col>
                        <Col span={8}>
                          <Form.Item
                            name="workspaceId"
                            label="Workspace ID"
                            rules={[{ required: true, message: '请输入 Workspace ID' }]}
                          >
                            <Input placeholder="workspace-id" />
                          </Form.Item>
                        </Col>
                      </Row>
                      <Form.Item>
                        <Button
                          type="primary"
                          htmlType="submit"
                          loading={ragEvalLoading}
                          icon={<PlayCircleOutlined />}
                          disabled={!cases || cases.length === 0}
                        >
                          开始评测 ({cases?.length || 0} 个用例)
                        </Button>
                      </Form.Item>
                    </Form>

                    <Spin spinning={ragEvalLoading}>
                      {ragEvalResult && (
                        <>
                          <Descriptions title="评测指标汇总" bordered size="small" column={3}>
                            <Descriptions.Item label="总用例数">
                              {ragEvalResult.metrics.total_test_cases ?? '-'}
                            </Descriptions.Item>
                            <Descriptions.Item label="成功用例">
                              {ragEvalResult.metrics.successful_cases ?? '-'}
                            </Descriptions.Item>
                            <Descriptions.Item label="综合评分">
                              {ragEvalResult.metrics.overall_score != null
                                ? ragEvalResult.metrics.overall_score.toFixed(2)
                                : '-'}
                            </Descriptions.Item>
                            <Descriptions.Item label="平均正确性">
                              {ragEvalResult.metrics.avg_correctness != null
                                ? ragEvalResult.metrics.avg_correctness.toFixed(3)
                                : '-'}
                            </Descriptions.Item>
                            <Descriptions.Item label="平均忠实度">
                              {ragEvalResult.metrics.avg_faithfulness != null
                                ? ragEvalResult.metrics.avg_faithfulness.toFixed(3)
                                : '-'}
                            </Descriptions.Item>
                            <Descriptions.Item label="平均相关性">
                              {ragEvalResult.metrics.avg_relevance != null
                                ? ragEvalResult.metrics.avg_relevance.toFixed(3)
                                : '-'}
                            </Descriptions.Item>
                            <Descriptions.Item label="平均连贯性">
                              {ragEvalResult.metrics.avg_coherence != null
                                ? ragEvalResult.metrics.avg_coherence.toFixed(3)
                                : '-'}
                            </Descriptions.Item>
                            <Descriptions.Item label="平均响应时间">
                              {ragEvalResult.metrics.avg_response_time_ms != null
                                ? (ragEvalResult.metrics.avg_response_time_ms / 1000).toFixed(2) + ' s'
                                : '-'}
                            </Descriptions.Item>
                          </Descriptions>

                          <div>
                            <Title level={5}>详细结果</Title>
                            <Table
                              rowKey={(_, idx) => idx?.toString() || '0'}
                              size="small"
                              dataSource={ragEvalResult.testCases}
                              pagination={{ pageSize: 10 }}
                              columns={[
                                { title: '问题', dataIndex: 'query', width: 200, ellipsis: true },
                                {
                                  title: '正确性',
                                  dataIndex: 'correctness',
                                  width: 80,
                                  render: (v: number) => (v != null ? v.toFixed(3) : '-'),
                                },
                                {
                                  title: '忠实度',
                                  dataIndex: 'faithfulness',
                                  width: 80,
                                  render: (v: number) => (v != null ? v.toFixed(3) : '-'),
                                },
                                {
                                  title: '相关性',
                                  dataIndex: 'relevance',
                                  width: 80,
                                  render: (v: number) => (v != null ? v.toFixed(3) : '-'),
                                },
                                {
                                  title: '连贯性',
                                  dataIndex: 'coherence',
                                  width: 80,
                                  render: (v: number) => (v != null ? v.toFixed(3) : '-'),
                                },
                                {
                                  title: '耗时(ms)',
                                  dataIndex: 'responseTimeMs',
                                  width: 80,
                                  render: (v: number) => (v != null ? v : '-'),
                                },
                              ]}
                              expandable={{
                                expandedRowRender: (record) => (
                                  <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                                    <div>
                                      <strong>模型答案：</strong>
                                      <pre
                                        style={{
                                          background: '#fafafa',
                                          padding: 8,
                                          whiteSpace: 'pre-wrap',
                                          maxHeight: 200,
                                          overflow: 'auto',
                                        }}
                                      >
                                        {record.modelAnswer || '-'}
                                      </pre>
                                    </div>
                                    <div>
                                      <strong>Ground Truth：</strong>
                                      <pre
                                        style={{
                                          background: '#fafafa',
                                          padding: 8,
                                          whiteSpace: 'pre-wrap',
                                          maxHeight: 200,
                                          overflow: 'auto',
                                        }}
                                      >
                                        {record.groundTruthAnswer || '-'}
                                      </pre>
                                    </div>
                                    {record.error && (
                                      <div>
                                        <strong style={{ color: 'red' }}>错误：</strong>
                                        <p>{record.error}</p>
                                      </div>
                                    )}
                                  </div>
                                ),
                              }}
                            />
                          </div>
                        </>
                      )}
                    </Spin>
                  </div>
                ),
              },
              {
                key: 'dataset-builder',
                label: '数据构建',
                children: (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 24, width: '100%' }}>
                    <Card title="评测数据集构建" size="small">
                      <Form
                        layout="vertical"
                        form={datasetBuilderForm}
                        onFinish={handleBuildDataset}
                      >
                        <Form.Item
                          label="文档内容（每行一个文档）"
                          required
                        >
                          <Input.TextArea
                            rows={6}
                            placeholder="输入文档内容，每行一个文档..."
                            value={buildDocuments.join('\n')}
                            onChange={(e) => {
                              // 直接保存所有行，包括空行
                              const lines = e.target.value.split('\n');
                              setBuildDocuments(lines);
                            }}
                          />
                        </Form.Item>
                        <Form.Item
                          name="targetSize"
                          label="目标数据集大小"
                          initialValue={100}
                        >
                          <InputNumber min={10} max={10000} />
                        </Form.Item>
                        <Form.Item>
                          <Button
                            type="primary"
                            htmlType="submit"
                            loading={buildingDataset}
                            icon={<PlayCircleOutlined />}
                          >
                            构建数据集
                          </Button>
                        </Form.Item>
                      </Form>
                    </Card>

                    {buildResult && (
                      <Spin spinning={buildingDataset}>
                        <Card title="构建结果" size="small">
                          <Row gutter={16} style={{ marginBottom: 24 }}>
                            <Col span={6}>
                              <Statistic
                                title="总数据量"
                                value={buildResult.totalCount}
                              />
                            </Col>
                            <Col span={6}>
                              <Statistic
                                title="Synthetic"
                                value={buildResult.sourceDistribution['synthetic'] || 0}
                              />
                            </Col>
                            <Col span={6}>
                              <Statistic
                                title="Real"
                                value={buildResult.sourceDistribution['real'] || 0}
                              />
                            </Col>
                            <Col span={6}>
                              <Statistic
                                title="Adversarial"
                                value={buildResult.sourceDistribution['adversarial'] || 0}
                              />
                            </Col>
                          </Row>

                          <Row gutter={16} style={{ marginBottom: 24 }}>
                            <Col span={8}>
                              <Card size="small" title="难度分布">
                                {Object.entries(buildResult.difficultyDistribution).map(([k, v]) => (
                                  <div key={k} style={{ marginBottom: 8 }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                                      <span>{k}</span>
                                      <span>{v}</span>
                                    </div>
                                    <Progress
                                      percent={Math.round((v / buildResult.totalCount) * 100)}
                                      size="small"
                                    />
                                  </div>
                                ))}
                              </Card>
                            </Col>
                            <Col span={8}>
                              <Card size="small" title="类型分布">
                                {Object.entries(buildResult.categoryDistribution).map(([k, v]) => (
                                  <div key={k} style={{ marginBottom: 8 }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                                      <span>{k}</span>
                                      <span>{v}</span>
                                    </div>
                                    <Progress
                                      percent={Math.round((v / buildResult.totalCount) * 100)}
                                      size="small"
                                    />
                                  </div>
                                ))}
                              </Card>
                            </Col>
                            <Col span={8}>
                              <Card size="small" title="来源分布">
                                {Object.entries(buildResult.sourceDistribution).map(([k, v]) => (
                                  <div key={k} style={{ marginBottom: 8 }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                                      <span>{k}</span>
                                      <span>{v}</span>
                                    </div>
                                    <Progress
                                      percent={Math.round((v / buildResult.totalCount) * 100)}
                                      size="small"
                                    />
                                  </div>
                                ))}
                              </Card>
                            </Col>
                          </Row>

                          <Card size="small" title="生成的数据样本" style={{ marginBottom: 16 }} extra={
                            <Button 
                              type="primary" 
                              size="small"
                              onClick={() => exportToExcel(buildResult.data)}
                            >
                              导出Excel
                            </Button>
                          }>
                            <Table
                              rowKey={(_, idx) => idx}
                              size="small"
                              dataSource={buildResult.data}
                              pagination={{
                                current: buildResultPage,
                                pageSize: buildResultPageSize,
                                total: buildResult.totalCount,
                                onChange: (page, pageSize) => {
                                  setBuildResultPage(page);
                                  setBuildResultPageSize(pageSize);
                                },
                                showSizeChanger: true,
                                pageSizeOptions: ['10', '20', '50', '100'],
                                showTotal: (total) => `共 ${total} 条数据`,
                              }}
                              columns={[
                                {
                                  title: '问题',
                                  dataIndex: 'query',
                                  ellipsis: true,
                                  render: (t: string) => (t.length > 100 ? t.slice(0, 100) + '...' : t),
                                },
                                {
                                  title: '难度',
                                  dataIndex: 'difficulty',
                                  width: 80,
                                },
                                {
                                  title: '类型',
                                  dataIndex: 'category',
                                  width: 100,
                                },
                                {
                                  title: '来源',
                                  dataIndex: 'source',
                                  width: 80,
                                },
                              ]}
                              expandable={{
                                expandedRowRender: (record) => (
                                  <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                                    <div>
                                      <strong>问题：</strong>
                                      <div style={{ background: '#fafafa', padding: 8, borderRadius: 4, marginTop: 4 }}>
                                        {record.query}
                                      </div>
                                    </div>
                                    <div>
                                      <strong>Ground Truth 文档：</strong>
                                      <div style={{ background: '#fafafa', padding: 8, borderRadius: 4, marginTop: 4 }}>
                                        {record.groundTruthDocs.map((doc, idx) => (
                                          <div key={idx} style={{ marginBottom: 8, paddingBottom: 8, borderBottom: '1px solid #e8e8e8' }}>
                                            {doc}
                                          </div>
                                        ))}
                                      </div>
                                    </div>
                                  </div>
                                ),
                              }}
                            />
                          </Card>
                        </Card>
                      </Spin>
                    )}
                  </div>
                ),
              },
            ]}
          />
        </Content>
      </Layout>
    </Layout>
  );
}

export default App;
