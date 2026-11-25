import {
  Button, Center, Checkbox, Container, Grid, Group, Loader, MultiSelect,
  RingProgress, SimpleGrid, Stack, TextInput,
} from '@mantine/core';
import { AreaChart } from '@mantine/charts';
import {
  IconAlertTriangle, IconChartBar, IconChecks, IconDownload,
  IconFilter, IconUserOff,
} from '@tabler/icons-react';
import { useCallback, useEffect, useMemo, useState, useContext } from 'react';
import type { CSSProperties } from 'react';
import { notifications } from '@mantine/notifications';
import { TourPageGate } from '../components/Tour';
import { StatCard } from '../components/StatCard';
import { Panel, StatusPill, MiniBar, EmptyState, SkeletonRows } from '../components/ui';
import { STATUS_META } from '../components/ui/status';
import type { StatusKey } from '../components/ui/status';
import { api } from '../services/api';
import { AuthContext } from '../contexts/AuthContext';
import type { Demand } from './Demands';

interface OrgUnit { id: string; name: string; code: string; }
interface Specialty { id: string; code: string; name: string; }
interface Page<T> { content: T[]; totalElements: number; totalPages: number; }

type Option = { value: string; label: string };

interface Breakdown {
  id: string;
  name: string;
  total: number;
  concluded: number;
  pending: number;
  interrupted: number;
  unassigned: number;
  resolutionRate: number;
  sharePercent: number;
}

interface DemandRow {
  id: string;
  protocol: string;
  title: string;
  departmentName: string;
  categoryCode: string;
  statusLabel: string;
  status: string;
  technicianName: string | null;
  ageHours: number;
}

interface AdvancedReport {
  generatedAt: string;
  period: { label: string };
  scope: string;
  metrics: {
    total: number;
    concluded: number;
    pending: number;
    interrupted: number;
    unassigned: number;
    resolutionRate: number;
    pendingRate: number;
    interruptionRate: number;
    unassignedRate: number;
    avgResolutionHours: number;
  };
  statusCounts: Record<string, number>;
  departments: Breakdown[];
  categories: Breakdown[];
  timelineData: Array<{ period: string; opened: number; concluded: number }>;
  criticalDemands: DemandRow[];
  demands: DemandRow[];
}

function buildPrintHTML(r: AdvancedReport): string {
  const rowsHTML = r.demands.map(d => `
    <tr>
      <td>${d.protocol}</td>
      <td>${d.title}</td>
      <td>${d.departmentName}</td>
      <td>${d.statusLabel}</td>
      <td>${d.technicianName ?? 'Sem técnico'}</td>
      <td>${d.ageHours}h</td>
    </tr>`).join('');

  const deptsHTML = r.departments.map(d => `
    <tr>
      <td>${d.name}</td><td>${d.total}</td><td>${d.concluded}</td>
      <td>${d.pending}</td><td>${d.unassigned}</td><td>${d.resolutionRate}%</td>
    </tr>`).join('');

  return `
    <h2 style="color:#2f9e44;margin-bottom:4px">Relatório Gerencial SAGED</h2>
    <p style="color:#666;font-size:10px">Gerado em: ${new Date(r.generatedAt).toLocaleString('pt-BR')} | Período: ${r.period.label} | Escopo: ${r.scope}</p>
    <hr/>
    <h3>Indicadores</h3>
    <table border="1" cellpadding="4" cellspacing="0" width="100%" style="border-collapse:collapse">
      <tr><th>Total</th><th>Concluídas</th><th>Pendentes</th><th>Interrompidas</th><th>Sem técnico</th><th>Taxa resolução</th></tr>
      <tr>
        <td>${r.metrics.total}</td><td>${r.metrics.concluded}</td><td>${r.metrics.pending}</td>
        <td>${r.metrics.interrupted}</td><td>${r.metrics.unassigned}</td><td>${r.metrics.resolutionRate}%</td>
      </tr>
    </table>
    <br/>
    <h3>Por Secretaria</h3>
    <table border="1" cellpadding="4" cellspacing="0" width="100%" style="border-collapse:collapse">
      <tr><th>Nome</th><th>Total</th><th>Concluídas</th><th>Pendentes</th><th>Sem técnico</th><th>Resolução</th></tr>
      ${deptsHTML}
    </table>
    <br/>
    <h3>Detalhamento de Demandas</h3>
    <table border="1" cellpadding="4" cellspacing="0" width="100%" style="border-collapse:collapse">
      <tr><th>Protocolo</th><th>Título</th><th>Secretaria</th><th>Status</th><th>Técnico</th><th>Idade</th></tr>
      ${rowsHTML}
    </table>`;
}

const STATUS_LABELS: Record<string, string> = {
  TODO: 'A Fazer',
  IN_PROGRESS: 'Em Andamento',
  DONE: 'Concluído',
  INTERRUPTED: 'Interrompido',
};

function todayInput() { return new Date().toISOString().slice(0, 10); }
function monthStartInput() {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10);
}
function pct(part: number, total: number) {
  return total > 0 ? Math.round((part / total) * 100) : 0;
}
function ageHours(createdAt: string) {
  return Math.round((Date.now() - new Date(createdAt).getTime()) / 3_600_000);
}

function buildReport(
  demands: Demand[],
  orgUnits: OrgUnit[],
  specialties: Specialty[],
  startDate: string,
  endDate: string,
  departmentIds: string[],
  statuses: string[],
  techTypeCodes: string[],
  onlyUnassigned: boolean,
  onlyCritical: boolean,
  includeDemandList: boolean,
): AdvancedReport {
  const start = new Date(startDate);
  const end = new Date(endDate + 'T23:59:59');

  let filtered = demands.filter(d => {
    const date = new Date(d.createdAt);
    return date >= start && date <= end;
  });

  if (departmentIds.length > 0) filtered = filtered.filter(d => departmentIds.includes(d.departmentId));
  if (statuses.length > 0) filtered = filtered.filter(d => statuses.includes(d.status));
  if (techTypeCodes.length > 0) filtered = filtered.filter(d => techTypeCodes.includes(d.specialtyId));
  if (onlyUnassigned) filtered = filtered.filter(d => !d.assigneeUserId);
  if (onlyCritical) filtered = filtered.filter(d => d.status === 'INTERRUPTED');

  const total = filtered.length;
  const concluded = filtered.filter(d => d.status === 'DONE').length;
  const pending = filtered.filter(d => d.status === 'TODO' || d.status === 'IN_PROGRESS').length;
  const interrupted = filtered.filter(d => d.status === 'INTERRUPTED').length;
  const unassigned = filtered.filter(d => !d.assigneeUserId).length;

  const statusCounts: Record<string, number> = {
    TODO: filtered.filter(d => d.status === 'TODO').length,
    IN_PROGRESS: filtered.filter(d => d.status === 'IN_PROGRESS').length,
    DONE: concluded,
    INTERRUPTED: interrupted,
  };

  const deptBreakdown: Breakdown[] = orgUnits.map(ou => {
    const sub = filtered.filter(d => d.departmentId === ou.id);
    const c = sub.filter(d => d.status === 'DONE').length;
    const p = sub.filter(d => d.status === 'TODO' || d.status === 'IN_PROGRESS').length;
    const it = sub.filter(d => d.status === 'INTERRUPTED').length;
    const ua = sub.filter(d => !d.assigneeUserId).length;
    return { id: ou.id, name: ou.name, total: sub.length, concluded: c, pending: p, interrupted: it, unassigned: ua, resolutionRate: pct(c, sub.length), sharePercent: pct(sub.length, total) };
  }).filter(b => b.total > 0).sort((a, b) => b.total - a.total);

  const catBreakdown: Breakdown[] = specialties.map(sp => {
    const sub = filtered.filter(d => d.specialtyId === sp.id);
    const c = sub.filter(d => d.status === 'DONE').length;
    const p = sub.filter(d => d.status === 'TODO' || d.status === 'IN_PROGRESS').length;
    const it = sub.filter(d => d.status === 'INTERRUPTED').length;
    const ua = sub.filter(d => !d.assigneeUserId).length;
    return { id: sp.id, name: sp.name, total: sub.length, concluded: c, pending: p, interrupted: it, unassigned: ua, resolutionRate: pct(c, sub.length), sharePercent: pct(sub.length, total) };
  }).filter(b => b.total > 0).sort((a, b) => b.total - a.total);

  // Timeline semanal
  const weeks: Array<{ period: string; opened: number; concluded: number }> = [];
  const span = Math.max(1, Math.ceil((end.getTime() - start.getTime()) / (7 * 86400000)));
  for (let w = 0; w < Math.min(span, 8); w++) {
    const ws = new Date(start.getTime() + w * 7 * 86400000);
    const we = new Date(ws.getTime() + 7 * 86400000);
    const opened = filtered.filter(d => { const dt = new Date(d.createdAt); return dt >= ws && dt < we; }).length;
    const done = filtered.filter(d => { const dt = new Date(d.createdAt); return dt >= ws && dt < we && d.status === 'DONE'; }).length;
    weeks.push({ period: `Sem ${w + 1}`, opened, concluded: done });
  }

  const toDemandRow = (d: Demand): DemandRow => ({
    id: d.id,
    protocol: d.protocol,
    title: d.title,
    departmentName: orgUnits.find(ou => ou.id === d.departmentId)?.name ?? 'Sem setor',
    categoryCode: specialties.find(sp => sp.id === d.specialtyId)?.name ?? d.specialtyId ?? '',
    statusLabel: STATUS_LABELS[d.status] ?? d.status,
    status: d.status,
    technicianName: d.technician?.name ?? null,
    ageHours: ageHours(d.createdAt),
  });

  const criticalDemands = filtered.filter(d => d.status === 'INTERRUPTED').map(toDemandRow);
  const allRows = includeDemandList ? filtered.map(toDemandRow) : [];

  return {
    generatedAt: new Date().toISOString(),
    period: { label: `${startDate} → ${endDate}` },
    scope: departmentIds.length === 0 ? 'Todas as unidades' : deptBreakdown.map(b => b.name).join(', '),
    metrics: { total, concluded, pending, interrupted, unassigned, resolutionRate: pct(concluded, total), pendingRate: pct(pending, total), interruptionRate: pct(interrupted, total), unassignedRate: pct(unassigned, total), avgResolutionHours: 0 },
    statusCounts,
    departments: deptBreakdown,
    categories: catBreakdown,
    timelineData: weeks,
    criticalDemands,
    demands: allRows,
  };
}

export function Reports() {
  const { user } = useContext(AuthContext);
  const isAdminGeral = user?.role === 'SAGED_ADMIN_GERAL';

  const [orgUnits, setOrgUnits] = useState<OrgUnit[]>([]);
  const [specialties, setSpecialties] = useState<Specialty[]>([]);
  const [allDemands, setAllDemands] = useState<Demand[]>([]);
  const [loadingOptions, setLoadingOptions] = useState(true);
  const [loadingReport, setLoadingReport] = useState(false);
  const [report, setReport] = useState<AdvancedReport | null>(null);

  const [startDate, setStartDate] = useState(monthStartInput());
  const [endDate, setEndDate] = useState(todayInput());
  const [departmentIds, setDepartmentIds] = useState<string[]>([]);
  const [statuses, setStatuses] = useState<string[]>([]);
  const [techTypeCodes, setTechTypeCodes] = useState<string[]>([]);
  const [onlyUnassigned, setOnlyUnassigned] = useState(false);
  const [onlyCritical, setOnlyCritical] = useState(false);
  const [includeDemandList, setIncludeDemandList] = useState(true);

  useEffect(() => {
    async function loadOptions() {
      try {
        setLoadingOptions(true);
        const [unitsRes, specsRes, demandsRes] = await Promise.all([
          api.get<OrgUnit[]>('/org-units'),
          api.get<Specialty[]>('/specialties'),
          api.get<Page<Demand>>('/demands', { params: { size: 500 } }),
        ]);
        setOrgUnits(unitsRes.data ?? []);
        setSpecialties(specsRes.data ?? []);
        setAllDemands(demandsRes.data?.content ?? []);
      } catch {
        notifications.show({ title: 'Erro', message: 'Não foi possível carregar dados do relatório.', color: 'red' });
      } finally {
        setLoadingOptions(false);
      }
    }
    loadOptions();
  }, []);

  const handleExportPDF = useCallback(() => {
    if (!report) return;
    const style = document.createElement('style');
    style.id = 'print-style';
    style.textContent = `
      @media print {
        body > *:not(#print-root) { display: none !important; }
        #print-root { display: block !important; }
        @page { size: A4; margin: 18mm; }
      }
    `;
    document.head.appendChild(style);

    const root = document.createElement('div');
    root.id = 'print-root';
    root.style.cssText = 'font-family: sans-serif; font-size: 11px; color: #111;';
    root.innerHTML = buildPrintHTML(report);
    document.body.appendChild(root);

    window.print();

    document.head.removeChild(style);
    document.body.removeChild(root);
  }, [report]);

  const fetchReport = useCallback(() => {
    setLoadingReport(true);
    try {
      const r = buildReport(allDemands, orgUnits, specialties, startDate, endDate, departmentIds, statuses, techTypeCodes, onlyUnassigned, onlyCritical, includeDemandList);
      setReport(r);
    } catch {
      notifications.show({ title: 'Erro ao gerar relatório', message: 'Revise os filtros e tente novamente.', color: 'red' });
    } finally {
      setLoadingReport(false);
    }
  }, [allDemands, orgUnits, specialties, startDate, endDate, departmentIds, statuses, techTypeCodes, onlyUnassigned, onlyCritical, includeDemandList]);

  useEffect(() => {
    if (!loadingOptions) fetchReport();
  }, [loadingOptions]); // eslint-disable-line react-hooks/exhaustive-deps

  const departmentOptions = useMemo<Option[]>(() => {
    if (!isAdminGeral && user?.departmentId) {
      const mine = orgUnits.find(u => u.id === user.departmentId);
      return mine ? [{ value: mine.id, label: `${mine.code} - ${mine.name}` }] : [];
    }
    return orgUnits.map(u => ({ value: u.id, label: `${u.code} - ${u.name}` }));
  }, [orgUnits, isAdminGeral, user?.departmentId]);

  const categoryOptions = useMemo<Option[]>(() => specialties.map(s => ({ value: s.id, label: `${s.code} - ${s.name}` })), [specialties]);

  const statusOptions: Option[] = [
    { value: 'TODO', label: 'A Fazer' },
    { value: 'IN_PROGRESS', label: 'Em Andamento' },
    { value: 'DONE', label: 'Concluído' },
    { value: 'INTERRUPTED', label: 'Interrompido' },
  ];


  const kpis = [
    { label: 'Demandas', value: report?.metrics.total ?? 0, icon: IconChartBar, color: '#1d720a',
      share: report && report.metrics.total > 0 ? 100 : 0, hint: 'no período filtrado' },
    { label: 'Resolução', value: `${report?.metrics.resolutionRate ?? 0}%`, icon: IconChecks, color: STATUS_META.DONE.color,
      share: report?.metrics.resolutionRate ?? 0, hint: `${report?.metrics.concluded ?? 0} ${report?.metrics.concluded === 1 ? "concluída" : "concluídas"}` },
    { label: 'Sem técnico', value: report?.metrics.unassigned ?? 0, icon: IconUserOff, color: STATUS_META.TODO.color,
      share: report?.metrics.unassignedRate ?? 0, hint: `${report?.metrics.unassignedRate ?? 0}% do total` },
    { label: 'Interrompidas', value: report?.metrics.interrupted ?? 0, icon: IconAlertTriangle, color: STATUS_META.INTERRUPTED.color,
      share: report?.metrics.interruptionRate ?? 0, hint: `${report?.metrics.interruptionRate ?? 0}% do total` },
  ];

  const ringSections = useMemo(() => {
    if (!report || report.metrics.total === 0) return [{ value: 100, color: '#edf1ee' }];
    return (Object.keys(STATUS_META) as StatusKey[])
      .filter(status => (report.statusCounts[status] ?? 0) > 0)
      .map(status => ({
        value: ((report.statusCounts[status] ?? 0) / report.metrics.total) * 100,
        color: STATUS_META[status].color,
      }));
  }, [report]);

  const resetFilters = () => {
    setStartDate(monthStartInput());
    setEndDate(todayInput());
    setDepartmentIds([]);
    setStatuses([]);
    setTechTypeCodes([]);
    setOnlyUnassigned(false);
    setOnlyCritical(false);
    setIncludeDemandList(true);
  };

  if (loadingOptions) {
    return (
      <div className="sg-page">
        <Center h="60vh"><Loader color="green" size="sm" /></Center>
      </div>
    );
  }

  return (
    <div className="sg-page">
      <TourPageGate phaseId="reports" />
      <Container size="xl" pt={44} pb={72}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', gap: 16, flexWrap: 'wrap' }}>
            <div>
              <div className="sg-eyebrow">
                <span className="sg-chip sg-chip--brand"><span className="sg-chip__dot" />Relatórios</span>
              </div>
              <h1 className="sg-h1">Relatório Gerencial Completo</h1>
              <p className="sg-lead">Monte filtros, visualize indicadores e acompanhe o desempenho operacional.</p>
            </div>
            <Group gap="sm">
              <Button leftSection={<IconFilter size={16} />} variant="default" radius="md" loading={loadingReport} onClick={fetchReport} data-tour="reports-filters">
                Pré-visualizar
              </Button>
              <Button leftSection={<IconDownload size={16} />} color="green.9" radius="md" disabled={!report} onClick={handleExportPDF} data-tour="reports-export">
                Exportar PDF
              </Button>
            </Group>
          </div>

          <Grid align="flex-start" gap="md">
            <Grid.Col span={{ base: 12, md: 4, lg: 3 }}>
              <Panel
                className="sg-filters"
                title="Filtros"
                subtitle="Período e escopo do relatório"
                action={<button type="button" className="sg-back" onClick={resetFilters}>Limpar</button>}
              >
                <div className="sg-section">
                  <div className="sg-filter-label">Período</div>
                  <SimpleGrid cols={2} spacing="xs">
                    <TextInput label="Inicial" type="date" value={startDate} onChange={e => setStartDate(e.currentTarget.value)} />
                    <TextInput label="Final" type="date" value={endDate} onChange={e => setEndDate(e.currentTarget.value)} />
                  </SimpleGrid>
                </div>
                <div className="sg-section">
                  <div className="sg-filter-label">Escopo</div>
                  <Stack gap="sm">
                    <MultiSelect
                      label="Secretarias"
                      placeholder="Todas permitidas"
                      data={departmentOptions}
                      value={departmentIds}
                      onChange={setDepartmentIds}
                      searchable
                      clearable
                      disabled={!isAdminGeral}
                    />
                    <MultiSelect label="Status" placeholder="Todos" data={statusOptions} value={statuses} onChange={setStatuses} clearable />
                    <MultiSelect label="Categoria de demanda" placeholder="Todas" data={categoryOptions} value={techTypeCodes} onChange={setTechTypeCodes} searchable clearable />
                  </Stack>
                </div>
                <div className="sg-section">
                  <div className="sg-filter-label">Opções</div>
                  <Stack gap="xs">
                    <Checkbox size="xs" label="Somente demandas sem técnico assumido" checked={onlyUnassigned} onChange={e => setOnlyUnassigned(e.currentTarget.checked)} />
                    <Checkbox size="xs" label="Somente demandas críticas/atenção" checked={onlyCritical} onChange={e => setOnlyCritical(e.currentTarget.checked)} />
                    <Checkbox size="xs" label="Incluir lista analítica de demandas" checked={includeDemandList} onChange={e => setIncludeDemandList(e.currentTarget.checked)} />
                  </Stack>
                </div>
              </Panel>
            </Grid.Col>

            <Grid.Col span={{ base: 12, md: 8, lg: 9 }}>
              {loadingReport ? (
                <Panel><SkeletonRows rows={6} /></Panel>
              ) : !report ? (
                <Panel><EmptyState>Gere uma prévia para visualizar o relatório.</EmptyState></Panel>
              ) : (
                <Stack gap="md">
                  <div className="sg-legend-inline" style={{ flexWrap: 'wrap' }}>
                    <span><strong style={{ color: 'var(--sg-text-2)', fontWeight: 600 }}>Período</strong> {fmtPeriod(startDate, endDate)}</span>
                    <span><strong style={{ color: 'var(--sg-text-2)', fontWeight: 600 }}>Escopo</strong> {report.scope}</span>
                    <span><strong style={{ color: 'var(--sg-text-2)', fontWeight: 600 }}>Gerado em</strong> {new Date(report.generatedAt).toLocaleString('pt-BR')}</span>
                  </div>

                  <SimpleGrid cols={{ base: 1, xs: 2, lg: 4 }} spacing="md" data-tour="reports-stats">
                    {kpis.map(kpi => (
                      <StatCard key={kpi.label} label={kpi.label} value={kpi.value} color={kpi.color} icon={kpi.icon} share={kpi.share} hint={kpi.hint} />
                    ))}
                  </SimpleGrid>

                  <Grid gap="md" align="stretch">
                    <Grid.Col span={{ base: 12, lg: 7 }} style={{ display: 'flex' }}>
                      <Panel
                        title="Fluxo do período"
                        subtitle="Chamados abertos e concluídos por semana"
                        style={{ flex: 1 }}
                        action={
                          <span className="sg-legend-inline">
                            <span><i style={{ background: '#1c7ed6' }} />Abertas</span>
                            <span><i style={{ background: STATUS_META.DONE.color }} />Concluídas</span>
                          </span>
                        }
                      >
                        {report.metrics.total === 0 ? (
                          <EmptyState icon={<IconChartBar size={18} stroke={1.6} />}>Sem chamados no período selecionado.</EmptyState>
                        ) : (
                          <AreaChart
                            h={300}
                            data={report.timelineData}
                            dataKey="period"
                            series={[
                              { name: 'opened', color: '#1c7ed6', label: 'Abertas' },
                              { name: 'concluded', color: STATUS_META.DONE.color, label: 'Concluídas' },
                            ]}
                            curveType="monotone"
                            withLegend={false}
                            tickLine="none"
                            gridAxis="x"
                            strokeWidth={2}
                            fillOpacity={0.12}
                            withDots
                            dotProps={{ r: 3, strokeWidth: 2 }}
                            yAxisProps={{ allowDecimals: false, width: 28 }}
                            gridProps={{ stroke: '#e4e9e5', strokeDasharray: '0' }}
                          />
                        )}
                      </Panel>
                    </Grid.Col>
                    <Grid.Col span={{ base: 12, lg: 5 }} style={{ display: 'flex' }}>
                      <Panel title="Distribuição por status" subtitle="Participação de cada situação no total" style={{ flex: 1 }}>
                        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 6 }}>
                          <RingProgress
                            size={150}
                            thickness={14}
                            rootColor="#edf1ee"
                            sections={ringSections}
                            label={
                              <div style={{ textAlign: 'center', lineHeight: 1.1 }}>
                                <div className="sg-num" style={{ fontSize: 26, fontWeight: 600, letterSpacing: '-.03em' }}>{report.metrics.total}</div>
                                <div style={{ fontSize: 11, color: 'var(--sg-muted)', marginTop: 3 }}>demandas</div>
                              </div>
                            }
                          />
                        </div>
                        <div className="sg-legend">
                          {(Object.keys(STATUS_META) as StatusKey[]).map(status => {
                            const count = report.statusCounts[status] ?? 0;
                            return (
                              <div className="sg-legend__row" key={status} style={{ '--kc': STATUS_META[status].color } as CSSProperties}>
                                <span className="sg-legend__dot" />
                                <span className="sg-legend__name">{STATUS_META[status].label}</span>
                                <span className="sg-legend__count">{count}</span>
                                <span className="sg-legend__pct">{pct(count, report.metrics.total)}%</span>
                              </div>
                            );
                          })}
                        </div>
                      </Panel>
                    </Grid.Col>
                  </Grid>

                  <BreakdownTable title="Resumo por secretaria" rows={report.departments} empty="Nenhuma secretaria no filtro." />
                  <BreakdownTable title="Categorias de demanda" rows={report.categories} empty="Nenhuma categoria no filtro." />

                  <Panel
                    title="Demandas críticas / atenção"
                    subtitle="Chamados interrompidos que exigem acompanhamento"
                    flush
                    action={<CountChip value={report.criticalDemands.length} tone="red" />}
                  >
                    <DemandTable rows={report.criticalDemands} empty="Nenhuma demanda crítica no período." />
                  </Panel>

                  {includeDemandList && (
                    <Panel
                      title="Detalhamento analítico"
                      subtitle="Lista completa dos chamados do período"
                      flush
                      action={<CountChip value={report.demands.length} tone="green" />}
                      footer={report.demands.length > 80 ? <span>Prévia limitada a 80 linhas.</span> : undefined}
                    >
                      <DemandTable rows={report.demands.slice(0, 80)} empty="Nenhuma demanda encontrada." />
                    </Panel>
                  )}
                </Stack>
              )}
            </Grid.Col>
          </Grid>
        </div>
      </Container>
    </div>
  );
}

function CountChip({ value, tone }: { value: number; tone: 'red' | 'green' }) {
  const style = tone === 'red'
    ? { background: '#ffecec', color: '#b02525', borderColor: '#ffd1d1' }
    : undefined;
  return <span className="sg-tag" style={style}>{value}</span>;
}

function BreakdownTable({ title, rows, empty }: { title: string; rows: Breakdown[]; empty: string }) {
  return (
    <Panel
      title={title}
      flush
      action={rows.length > 0 ? <CountChip value={rows.length} tone="green" /> : undefined}
    >
      {rows.length === 0 ? (
        <EmptyState>{empty}</EmptyState>
      ) : (
        <div className="sg-tablewrap">
          <table className="sg-table sg-table--metrics">
            <thead>
              <tr>
                <th>Nome</th>
                <th className="num">Total</th>
                <th className="num">Concluídas</th>
                <th className="num">Pendentes</th>
                <th className="num">Sem técnico</th>
                <th className="num">Carga</th>
                <th className="num">Resolução</th>
              </tr>
            </thead>
            <tbody>
              {rows.slice(0, 10).map(row => (
                <tr key={row.id}>
                  <td className="strong">{row.name}</td>
                  <td className="num strong">{row.total}</td>
                  <td className="num">{row.concluded}</td>
                  <td className="num">{row.pending}</td>
                  <td className={`num${row.unassigned === 0 ? ' muted' : ''}`}>{row.unassigned}</td>
                  <td className="num">
                    <div className="sg-cellbar">
                      <MiniBar value={row.sharePercent} color="#9aa8a0" />
                      <span style={{ minWidth: 36 }}>{row.sharePercent}%</span>
                    </div>
                  </td>
                  <td className="num">
                    <div className="sg-cellbar">
                      <MiniBar value={row.resolutionRate} color={STATUS_META.DONE.color} />
                      <span style={{ minWidth: 36 }}>{row.resolutionRate}%</span>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Panel>
  );
}

function formatAge(hours: number) {
  return hours >= 48 ? `${Math.round(hours / 24)} d` : `${hours} h`;
}

function DemandTable({ rows, empty }: { rows: DemandRow[]; empty: string }) {
  if (rows.length === 0) return <EmptyState>{empty}</EmptyState>;
  return (
    <div className="sg-tablewrap">
      <table className="sg-table">
        <thead>
          <tr>
            <th>Protocolo</th>
            <th>Título</th>
            <th>Secretaria</th>
            <th>Status</th>
            <th>Técnico</th>
            <th className="num">Idade</th>
          </tr>
        </thead>
        <tbody>
          {rows.map(row => (
            <tr key={row.id}>
              <td><span className="sg-proto">{row.protocol}</span></td>
              <td style={{ minWidth: 200 }}><span className="sg-clamp strong">{row.title}</span></td>
              <td className="muted">{row.departmentName}</td>
              <td><StatusPill status={row.status} /></td>
              <td className={row.technicianName ? undefined : 'muted'}>{row.technicianName ?? 'Sem técnico'}</td>
              <td className="num muted" title={`${row.ageHours} horas`}>{formatAge(row.ageHours)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function fmtPeriod(start: string, end: string) {
  const f = (iso: string) => iso.split('-').reverse().join('/');
  return `${f(start)} a ${f(end)}`;
}
