import { Container, Grid, RingProgress, SimpleGrid, Tooltip } from '@mantine/core';
import {
  IconClipboardList, IconPlayerPlay, IconCheck,
  IconAlertTriangle, IconChevronLeft, IconClock, IconBarrierBlock,
  IconListSearch,
} from '@tabler/icons-react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useState, useEffect, useMemo, useContext } from 'react';
import type { CSSProperties } from 'react';
import { api } from '../services/api';
import { StatCard } from '../components/StatCard';
import { AuthContext } from '../contexts/AuthContext';
import { TechActivity } from '../components/TechActivity';
import { TourPageGate } from '../components/Tour';
import { Panel, StatusPill, EmptyState, SkeletonRows } from '../components/ui';
import { STATUS_META } from '../components/ui/status';
import { buildDemandsUrl } from '../utils/demandNavigation';
import type { Demand } from '../pages/Demands';

interface OrgUnit { id: string; name: string; code: string; }

const RECENT_LIMIT = 5;

export function Dashboard() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [allDemands, setAllDemands] = useState<Demand[]>([]);
  const [orgUnits, setOrgUnits] = useState<OrgUnit[]>([]);
  const [loading, setLoading] = useState(true);

  const unitId = searchParams.get('unit');
  const unitName = searchParams.get('name') || 'Secretaria Selecionada';

  const { user } = useContext(AuthContext);


  const roleUpper = useMemo(() => user?.role?.trim().toUpperCase(), [user]);

  // Mapeamento das permissões de acordo com as especificações do Swagger
  const isAdminGeral = useMemo(() => roleUpper === 'SAGED_ADMIN_GERAL', [roleUpper]);
  const isTechnician = useMemo(() => roleUpper === 'SAGED_TECNICO', [roleUpper]);
  const hasSectorView = useMemo(() => {
    return roleUpper === 'SAGED_ADMIN_SETOR' || roleUpper === 'SAGED_TECNICO_LIDER' ;
  }, [roleUpper]);

  // Busca geral robusta baseada no token (o backend filtra o escopo automaticamente via RBAC)
  useEffect(() => {
    async function loadData() {
      setLoading(true);
      try {
        const params: Record<string, unknown> = { size: 200 };
        if (unitId && unitId !== 'geral') params.departmentId = unitId;
        const [demandsRes, unitsRes] = await Promise.all([
          api.get<{ content: Demand[] }>('/demands', { params }),
          api.get<OrgUnit[]>('/org-units'),
        ]);
        setAllDemands(Array.isArray(demandsRes.data.content) ? demandsRes.data.content : []);
        setOrgUnits(Array.isArray(unitsRes.data) ? unitsRes.data : []);
      } catch (error) {
        console.error('Erro ao buscar dados para o painel:', error);
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, [unitId]);

  // ESCOPO DO DASHBOARD: Sincronizado com o retorno real da API
  const demands = useMemo(() => {
    let result = allDemands;

    // Se o usuário selecionou uma unidade específica na barra de busca (Admin Geral)
    if (unitId && unitId !== 'geral') {
      result = result.filter(demand => String(demand.departmentId) === String(unitId));
    }

    // Nota: O backend já retorna apenas as demandas permitidas para técnicos
    // e líderes de setor via RBAC automático no endpoint /demands.
    return result;
  }, [allDemands, unitId]);

  // Resolve o nome da unidade para exibição no Badge
  const displayUnitName = useMemo(() => {
    if (unitId === 'geral' || !unitId) return 'ADMINISTRAÇÃO MUNICIPAL';
    if (unitName && unitName !== 'Secretaria Selecionada') return unitName;
    // Se tiver ID mas não tiver nome (vindo de um link direto), tenta achar no primeiro item da lista
    return demands[0]?.department?.name || 'UNIDADE FILTRADA';
  }, [unitId, unitName, demands]);

  // Indicadores calculados dinamicamente com base nas demandas acessíveis
  const metrics = useMemo(() => {
    const total = demands.length;
    if (total === 0) return { resolutionRate: 0, interruptedCount: 0, criticalPercentage: 0 };

    const completed = demands.filter(d => d.status === 'DONE').length;
    const interrupted = demands.filter(d => d.status === 'INTERRUPTED').length;

    const critical = interrupted;

    return {
      resolutionRate: Math.round((completed / total) * 100),
      interruptedCount: interrupted,
      criticalPercentage: Math.round((critical / total) * 100)
    };
  }, [demands]);

  const counters = useMemo(() => {
    return {
      aFazer: demands.filter(d => d.status === 'TODO').length,
      emAndamento: demands.filter(d => d.status === 'IN_PROGRESS').length,
      concluido: demands.filter(d => d.status === 'DONE').length,
      interrompido: demands.filter(d => d.status === 'INTERRUPTED').length,
    };
  }, [demands]);

  const totalCount =
    counters.aFazer + counters.emAndamento + counters.concluido + counters.interrompido;

  const priorityData = useMemo(() => {
    const pct = (count: number) => (totalCount > 0 ? Math.round((count / totalCount) * 100) : 0);

    return [
      { label: 'Aguardando início', count: counters.aFazer, color: STATUS_META.TODO.color, progress: pct(counters.aFazer) },
      { label: 'Em andamento', count: counters.emAndamento, color: STATUS_META.IN_PROGRESS.color, progress: pct(counters.emAndamento) },
      { label: 'Concluídos', count: counters.concluido, color: STATUS_META.DONE.color, progress: pct(counters.concluido) },
      { label: 'Retidas', count: counters.interrompido, color: STATUS_META.INTERRUPTED.color, progress: pct(counters.interrompido) },
    ];
  }, [counters, totalCount]);

  const share = (count: number) => (totalCount > 0 ? Math.round((count / totalCount) * 100) : 0);
  const unitNameOf = (departmentId: string) => orgUnits.find(u => u.id === departmentId)?.name;

  const scopeChip = isTechnician ? (
    <span className="sg-chip sg-chip--blue">
      <span className="sg-chip__dot" />
      Painel técnico: meus chamados
    </span>
  ) : hasSectorView ? (
    <span className="sg-chip sg-chip--brand">
      <span className="sg-chip__dot" />
      Fila da secretaria: {displayUnitName}
    </span>
  ) : isAdminGeral && unitId && unitId !== 'geral' ? (
    <span className="sg-chip sg-chip--brand">
      <span className="sg-chip__dot" />
      {displayUnitName}
    </span>
  ) : (
    <span className="sg-chip sg-chip--brand">
      <span className="sg-chip__dot" />
      Visão global administrativa (SAGED)
    </span>
  );

  const resolutionColor = STATUS_META.DONE.color;
  const stripVars = (c: string) => ({ '--kc': c, '--kc-soft': `${c}14` }) as CSSProperties;

  return (
    <div className="sg-page">
      <Container size="xl" pt={44} pb={72}>
        <TourPageGate phaseId="header-dashboard" />
        <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>

          <div>
            {isAdminGeral && (
              <button type="button" className="sg-back" style={{ marginBottom: 18 }} onClick={() => navigate('/selecionar-unidade')}>
                <IconChevronLeft size={15} />
                Voltar para seleção de unidade
              </button>
            )}

            <div className="sg-eyebrow">
              {scopeChip}
              <Tooltip label="Sincronizado em tempo real com o banco de dados">
                <span className="sg-chip" style={{ cursor: 'help' }}>
                  <span className="sg-chip__dot sg-chip__dot--live" />
                  Live
                </span>
              </Tooltip>
            </div>
            <h1 className="sg-h1">Painel de Performance e Produtividade</h1>
            <p className="sg-lead">Métricas operacionais extraídas do fluxo atual de chamados do sistema.</p>
          </div>

          <SimpleGrid cols={{ base: 1, xs: 2, lg: 4 }} spacing="md" data-tour="dashboard-metrics">
            <StatCard label="A Fazer" value={counters.aFazer} color={STATUS_META.TODO.color} icon={IconClipboardList}
              share={share(counters.aFazer)} hint={`${share(counters.aFazer)}% do total`} />
            <StatCard label="Em Andamento" value={counters.emAndamento} color={STATUS_META.IN_PROGRESS.color} icon={IconPlayerPlay}
              share={share(counters.emAndamento)} hint={`${share(counters.emAndamento)}% do total`} />
            <StatCard label="Concluído" value={counters.concluido} color={STATUS_META.DONE.color} icon={IconCheck}
              share={share(counters.concluido)} hint={`${share(counters.concluido)}% do total`} />
            <StatCard label="Interrompidas" value={counters.interrompido} color={STATUS_META.INTERRUPTED.color} icon={IconAlertTriangle}
              share={share(counters.interrompido)} hint={`${share(counters.interrompido)}% do total`} />
          </SimpleGrid>

          <Panel data-tour="dashboard-performance">
            <div className="sg-strip">
              <div className="sg-strip__cell" style={stripVars(resolutionColor)}>
                <div>
                  <div className="sg-strip__label">Taxa de resolução</div>
                  <div className="sg-strip__value" style={{ color: resolutionColor }}>{metrics.resolutionRate}%</div>
                  <div className="sg-strip__hint">Eficiência interna de chamados</div>
                </div>
                <RingProgress
                  size={60}
                  thickness={6}
                  roundCaps
                  rootColor="#edf1ee"
                  sections={[{ value: metrics.resolutionRate, color: resolutionColor }]}
                />
              </div>

              <div className="sg-strip__cell" style={stripVars(STATUS_META.INTERRUPTED.color)}>
                <div>
                  <div className="sg-strip__label">Demandas retidas</div>
                  <div className="sg-strip__value">{metrics.interruptedCount}</div>
                  <div className="sg-strip__hint">Aguardando peça ou suporte externo</div>
                </div>
                <span className="sg-strip__icon"><IconBarrierBlock size={20} stroke={1.7} /></span>
              </div>

              <div className="sg-strip__cell" style={stripVars(STATUS_META.IN_PROGRESS.color)}>
                <div>
                  <div className="sg-strip__label">Tempo de resposta</div>
                  <div className="sg-strip__value">Sub-1h</div>
                  <div className="sg-strip__hint">Estimativa de primeiro atendimento</div>
                </div>
                <span className="sg-strip__icon"><IconClock size={20} stroke={1.7} /></span>
              </div>
            </div>
          </Panel>

          <Grid columns={12} gap="md">
            <Grid.Col span={{ base: 12, md: 5, lg: 4 }} style={{ display: 'flex' }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16, flex: 1, minWidth: 0 }}>
              <Panel
                title="Distribuição de carga"
                subtitle="Volume de chamados por situação operacional"
                

                data-tour="dashboard-distribution"
              >
                {totalCount === 0 ? (
                  <EmptyState>Sem chamados para distribuir.</EmptyState>
                ) : (
                  <div className="sg-stack" role="img" aria-label="Distribuição de chamados por situação">
                    {priorityData.filter(i => i.count > 0).map(i => (
                      <i key={i.label} style={{ flex: `${i.count} 1 0`, background: i.color }} />
                    ))}
                  </div>
                )}
                <div className="sg-legend">
                  {priorityData.map(item => (
                    <div className="sg-legend__row" key={item.label} style={{ '--kc': item.color } as CSSProperties}>
                      <span className="sg-legend__dot" />
                      <span className="sg-legend__name">{item.label}</span>
                      <span className="sg-legend__count">{item.count}</span>
                      <span className="sg-legend__pct">{item.progress}%</span>
                    </div>
                  ))}
                </div>
              </Panel>
                <Panel
                  title="Atividade técnica ativa"
                  className="sg-panel--grow"
                  action={<span className="sg-chip"><span className="sg-chip__dot sg-chip__dot--live" />Monitoramento SAGED</span>}
                  flush
                  style={{ flex: 1 }}
                >
                  <TechActivity
                    techName={user?.name || "Técnico"}
                    demand={isTechnician ? "Visualização de Fila Operacional Pessoal" : "Supervisão Geral do Painel SAGED"}
                    status="ONLINE"
                    time="Agora"
                  />
                </Panel>
              </div>
            </Grid.Col>

            <Grid.Col span={{ base: 12, md: 7, lg: 8 }} style={{ display: 'flex' }}>
                <Panel
                  title="Fluxo recente de demandas vinculadas"
                  subtitle="Clique em uma linha para abrir o chamado no quadro"
                  flush
                  data-tour="dashboard-table"
                  className="sg-panel--stick"
                  style={{ flex: 1 }}
                  footer={
                    !loading && demands.length > 0 ? (
                      <>
                        <span>Exibindo {Math.min(RECENT_LIMIT, demands.length)} de {demands.length} chamados</span>
                      </>
                    ) : undefined
                  }
                >
                  {loading ? (
                    <SkeletonRows rows={RECENT_LIMIT} />
                  ) : demands.length === 0 ? (
                    <EmptyState icon={<IconListSearch size={18} stroke={1.6} />}>
                      Nenhuma demanda localizada para o escopo deste usuário.
                    </EmptyState>
                  ) : (
                    <div className="sg-tablewrap">
                      <table className="sg-table sg-table--click">
                        <thead>
                          <tr>
                            <th>Protocolo</th>
                            <th>Descrição da demanda</th>
                            <th>Status</th>
                          </tr>
                        </thead>
                        <tbody>
                          {demands.slice(0, RECENT_LIMIT).map((demand) => {
                            const sector = unitNameOf(demand.departmentId);
                            return (
                              //Entrada clicável para detalhes da demanda, passando o ID da unidade e nome via query params para o filtro na página de demandas
                              <tr
                                key={demand.id}
                                onClick={() => {
                                  const deptName = sector ?? 'Secretaria';
                                  navigate(buildDemandsUrl({ ...demand, department: { name: deptName } }));
                                }}
                              >
                                <td><span className="sg-proto">#{demand.protocol}</span></td>
                                <td>
                                  <span className="strong" style={{ display: 'block', maxWidth: 340, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                    {demand.title || demand.description}
                                  </span>
                                  {sector && <span className="sub">{sector}</span>}
                                </td>
                                <td><StatusPill status={demand.status} /></td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    </div>
                  )}
                </Panel>
            </Grid.Col>
          </Grid>
        </div>
      </Container>
    </div>
  );
}
