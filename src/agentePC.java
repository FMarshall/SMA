/************************************************************************************************************
 * @author Fernando Américo Albuquerque Rodrigues Marçal
 * @since 09/04/2015
 * @version 1.0
 * Descrição
 * Este é o comportamento do Agente do ponto de conexão (APC) 
 *
 * << Lista de Abreviaturas >>
 * nomeAgente: variável que receberá o nome do agente em questão
 * Mensagem: variável que receberá a mensagem ACLMessage
 * BD: banco de dados
 * CR : conexão a rede. Diz se a urrede está conectada a rede e consequente o estado da chave do PCC. 1-conectado/fechado 0-
 ************************************************************************************************************/


import jade.core.Agent;
//import java.util.Iterator;
import jade.lang.acl.ACLMessage; //Relacionada a endereçoes
import jade.core.AID;    //Relacionada a endereços
import jade.lang.acl.MessageTemplate; // Para uso dos filtros
import jade.proto.SubscriptionInitiator;
import jade.domain.FIPANames; //Foi solicitado no protocolo suscribe 
//import jade.core.behaviours.CyclicBehaviour; //Para comportamento temporal

import jade.core.behaviours.TickerBehaviour;






//Bibliotecas para lidar com arquivos XML
//import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder; //This package support classes for building JDOM documents and content using SAX parsers. 
//import org.jdom2.Attribute;






//Foram incluídas automaticamente
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

//Importados automaticamente. Para tratar de listas
import java.util.List;
import java.util.List; //Trantando com lista

public class agentePC extends Agent { /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

// Classe "agentePC" que por sua vez é uma subclasse
									// da classe "Agent"

	public void setup()
	{
		final String nomeAgente = getLocalName(); //a variável "nomeAgente" recebe o nome local do agente 
		final Element agenteApcBD = carregaBD(nomeAgente); //Chama o método carregaBD que carrega o BD do agente "nomeAgente"
		
		//Filtro para receber somente mensagens do protocolo tipo "inform"
		MessageTemplate filtroInformMonitoramento = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
		
//		System.out.println(".:: Agente PCC APCC1 iniciado com sucesso! ::.\n");
//		System.out.println("Todas as minhas informações: \n" +getAID());
//		System.out.println(">> Meu nome local é " + getLocalName()); // Informações completas
//		System.out.println("\n>> Meu nome global é " +getAID().getName());
//		System.out.println("Meu endereço é " +getAID().getAllAddresses());
		
//		System.out.println("\nMeus endereços são: ");
//		Iterator it = getAID().getAllAddresses();
//		while(it.hasNext()){
//			System.out.println("- "+it.next());
//		}

		addBehaviour(new TickerBehaviour(this,100) {
			public void onTick(){

				ACLMessage filtro_Inform = receive();
				//String conteudo = mensagem.getContent();

				//if(msg_curto!=null && msg_curto.getContent()=="curto"){
				//if(msg_curto!=null && conteudo=="curto"){
				if(filtro_Inform!=null){	
					exibirMensagem(filtro_Inform);
					
					if(filtro_Inform.getContent().equals("0")) {
						System.out.println("Chave está aberta!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
						
						agenteApcBD.getChild("cr").setText(filtro_Inform.getContent()); //Seta no XML o CR
						//Ao receber a mensagem Inform do estado do PCC, não é necessário responder a mensagem
//						ACLMessage resposta = filtro_Inform.createReply();
//						resposta.setPerformative(ACLMessage.AGREE);
//						resposta.setContent("Recebido!");
//						myAgent.send(resposta);
						
//						agenteApcBD.getChild("estado").setText("aberta");
						
<<<<<<< HEAD
//						String cr =  agenteApcBD.getChild("cr").getText(); //Dá no mesmo que a seguinte linha de código
						String cr =  agenteApcBD.getChildText("cr");
						System.out.println("O estado do PCC é "+cr+"." ); //Só pra testar se tá dando certo
						
						/*
						 * Eu pus um delay porque primeiro o APC, AG e AA devem receber os dados do Matlab para somente depois 
						 * o APC começar a analisar o balanço de energia e começar uma coordenação de tudo.
						 */
						try {
						    Thread.sleep(10000);                 //Delay em milisegundos
						} catch(InterruptedException ex) {
						    Thread.currentThread().interrupt();
						}
						
						if (cr.equals("0")){
//							System.out.println("A microrrede está desconectada!");
							
							/********************************************************************************
							 * FIPA Subscribe Initiator para saber o valor de Potência do DER e Carga
							 ********************************************************************************/	
							ACLMessage msgColetarPot = new ACLMessage(ACLMessage.SUBSCRIBE); // Campo da mensagem SUBSCRIBE
							msgColetarPot.setProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
							msgColetarPot.setContent("Potencia");
							
							/**
							 * Aqui vamos acessar o XML do APC para pesquisar o nome dos agentes de geração intermitente
							 * e cargas para calcular o balanço de potência na microrrede
							 * */
							List lista = agenteApcBD.getChild("agentesGeracaoNaoControladas").getChildren(); 
//							System.out.println("O nome dos AGs intermitentes são: "+lista);
							Iterator i = lista.iterator();
							
						    while(i.hasNext()) {
						    	Element elemento = (Element) i.next();

//						    	String nome = String.valueOf(elemento.getText());
//						    	String nome = String.valueOf(elemento.getChild();
						    	String nome = String.valueOf(elemento.getName());
						    	System.out.println("O nome é: "+nome);
						    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){
									System.out.println("Entrou no ifl!!!!!");
						    		//quantChaves();
//						    		cont1 = cont1 + 1;
						    		System.out.println("-"+getLocalName()+": Solicitando valor de carga a "+nome);
						    		msgColetarPot.addReceiver(new AID((String) nome, AID.ISLOCALNAME));
						    		//System.out.println("o valor atual de cont1 �: "+cont1);
						    	}
						    }
						    
						    addBehaviour(new SubscriptionInitiator(myAgent,msgColetarPot){
						   
								private static final long serialVersionUID = 1L;

								protected void handleAgree(ACLMessage agree){
						    		
						    	}//Fim do handleAgree do Subscribe
								
								protected void handleRefuse(ACLMessage refuse) {
									//Se recusar
								}
								protected void handleFailure(ACLMessage failure) {
									//Erro
								}
						    }); // Fim do comportamento FIPA Subscribe -> addBehaviour(new SubscriptionInitiator(myAgent,msgColetarPot){
						    //Obs.: Por enquanto vou colocar valores aleatórios para cacular o balanço de potência na microrrede
						    // Mas ai tenho que já ter uma base da potência da microrrede, das cargas...
							
							/**
							 * Aqui supõe-se que eu já enviei um subscribe para o AG intermitente e cargas
							 * e tenho o valor do balanço de potência na microrrede (deltaP)
							 */
							double deltaP = -1000; //Balanço de potência na microrrede. 
//							System.out.println("O valor do balanço é "+deltaP); //Só pra testar

							/**
							 * Um valor positivo de deltaP diz que tá sobrando energia. Um valor negativo, diz que tá com déficit, ou seja, 
							 * não tem geração suficiente.
							 * Se deltaP positivo tá tudo ok. 
							 * Se deltaP negativo, então verifica-se se tem dispositivos armazenadores de energia. 
							 *  */
							if(deltaP<0){ 
								/**se entrar nesse if, será necessário consultar o XMl para averiguar se há dispositivos
								 * armazenadores de energia
								 */
//								List lista = agenteChaveBD.getChild("sentido").getChild("sentido2").getChild("outrasChaves").getChildren(); 
		//
//								Iterator i = lista.iterator();
		//
//								while(i.hasNext()) {
//									Element elemento = (Element) i.next();
//									String nome = String.valueOf(elemento.getText());
//									if (nome!= null && nome.length()>0 && nome!= "nenhum"){
//										
//										cont1 = cont1 + 1;
//										System.out.println("As outras chaves s�o: "+nome);
//										msg.addReceiver(new AID((String) nome, AID.ISLOCALNAME));
		//
//									}
//								}// fim do while(i.hasNext())
								
//								msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
//								msg.setContent("abrir");
								
								/**
								 * Se tiver dispositivos armazenadores, manda-se um contracte net para todos ao mesmo tempo 
								 * solicitando um deltaP. Devo pegar um pouco de cada ou pego o de uma vez só? Como sei que só terá 
								 * um banco de baterias vou fazer pegando só de um dispositivo de armazenamento logo.
								 */
								/*if(){   //Se tem dispositivos armazenadores de energia, então
								*//*********************************************************************************
								 * FIPA Contract Net Initiator para negociação deltaP com sistemas de armazenamento de energia
								 *********************************************************************************//*
									ACLMessage negociar = new ACLMessage(ACLMessage.CFP);
									List lista1 = agenteAlimentadorBD.getChild("outrosALs").getChildren(); 
									Iterator i = lista1.iterator();
			
			
									while(i.hasNext()) {
			
										Element elemento = (Element) i.next();
			
										//String nome = String.valueOf(elemento.getText());
										String nome = String.valueOf(elemento.getText());
										System.out.println("-<<"+agenteAlimentador+">>: o outro AL � "+nome);
										//negociar.addReceiver(new AID((String) nome, AID.ISLOCALNAME));
										negociar.addReceiver(new AID( nome, AID.ISLOCALNAME));
			
									}
			
									// String carga = agenteAlimentadorBD.getChild("cargaPerdida").getAttributeValue("valor");
									negociar.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
									negociar.setContent(carga); //Carga perdida
			
									addBehaviour(new ContractNetInitiator(myAgent, negociar) {
			
										protected void handlePropose(ACLMessage propose, Vector v) {
											System.out.println("-<<"+agenteAlimentador+">>: o agente "+propose.getSender().getLocalName()+" disse "+propose.getContent());
										}
			
										protected void handleRefuse(ACLMessage refuse) {
											System.out.println("-<<"+agenteAlimentador+">>: o agente "+refuse.getSender().getLocalName()+" recusou");
										}
			
										protected void handleFailure(ACLMessage failure) {
											if (failure.getSender().equals(myAgent.getAMS())) {
			
												System.out.println("N�o existe outros agentes ALs");
											}
											else {
												System.out.println("-<<"+agenteAlimentador+">>: o agente "+failure.getSender().getLocalName()+" falhou");
											}
											// Immediate failure --> we will not receive a response from this agent
										}
			
										protected void handleAllResponses(Vector responses, Vector acceptances) {
			
										}// fim do handleAllResponses do comportamento Contract net
									}// fim do if para ver se 
		*/					}// fim do if(deltaP<0) 
=======

>>>>>>> sincronizar
					}else if(filtro_Inform.getContent().equals("1")){
						System.out.println("Chave está fechada!!!!!!!");
					}else{
						System.out.println("Deu pau!");
					}
					
					
//					}// fim do 	if get.content = 0
					
				}// fim o if para saber se inform != null
			} // fim do onTick 
		}); //fim do comportamento temporal TickerBehaviour
		
		addBehaviour(new TickerBehaviour(this,100){
			public void onTick(){
//				String cr =  agenteApcBD.getChild("cr").getText(); //Dá no mesmo que a seguinte linha de código
				String cr =  agenteApcBD.getChildText("cr");
				System.out.println("O estado do PCC é "+cr+"." ); //Só pra testar se tá dando certo
				
				if (cr.equals("0")){
//					System.out.println("A microrrede está desconectada!");
					
					/********************************************************************************
					 * FIPA Subscribe Initiator para saber o valor de Potência do DER e Carga
					 ********************************************************************************/	
					ACLMessage msgColetarPot = new ACLMessage(ACLMessage.SUBSCRIBE); // Campo da mensagem SUBSCRIBE
					msgColetarPot.setProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
					msgColetarPot.setContent("Potencia");
					
					/**
					 * Aqui vamos acessar o XML do APC para pesquisar o nome dos agentes de geração intermitente
					 * e cargas para calcular o balanço de potência na microrrede
					 * */
					List lista = agenteApcBD.getChild("agentesGeracaoNaoControladas").getChildren(); 
//					System.out.println("O nome dos AGs intermitentes são: "+lista);
					Iterator i = lista.iterator();
					
				    while(i.hasNext()) {
				    	Element elemento = (Element) i.next();

//				    	String nome = String.valueOf(elemento.getText());
//				    	String nome = String.valueOf(elemento.getChild();
				    	String nome = String.valueOf(elemento.getName());
				    	System.out.println("O nome é: "+nome);
				    	if (nome!= null && nome.length()>0 && nome!= "nenhum"){
							System.out.println("Entrou no ifl!!!!!");
				    		//quantChaves();
//				    		cont1 = cont1 + 1;
				    		System.out.println("-"+getLocalName()+": Solicitando valor de carga a "+nome);
				    		msgColetarPot.addReceiver(new AID((String) nome, AID.ISLOCALNAME));
				    		//System.out.println("o valor atual de cont1 �: "+cont1);
				    	}
				    }
				    
				    addBehaviour(new SubscriptionInitiator(myAgent,msgColetarPot){
				   
						private static final long serialVersionUID = 1L;

						protected void handleAgree(ACLMessage agree){
				    		
				    	}//Fim do handleAgree do Subscribe
						
						protected void handleRefuse(ACLMessage refuse) {
							//Se recusar
						}
						protected void handleFailure(ACLMessage failure) {
							//Erro
						}
				    }); // Fim do comportamento FIPA Subscribe -> addBehaviour(new SubscriptionInitiator(myAgent,msgColetarPot){
				    //Obs.: Por enquanto vou colocar valores aleatórios para cacular o balanço de potência na microrrede
				    // Mas ai tenho que já ter uma base da potência da microrrede, das cargas...
					
					/**
					 * Aqui supõe-se que eu já enviei um subscribe para o AG intermitente e cargas
					 * e tenho o valor do balanço de potência na microrrede (deltaP)
					 */
					double deltaP = -1000; //Balanço de potência na microrrede. 
//					System.out.println("O valor do balanço é "+deltaP); //Só pra testar

					/**
					 * Um valor positivo de deltaP diz que tá sobrando energia. Um valor negativo, diz que tá com déficit, ou seja, 
					 * não tem geração suficiente.
					 * Se deltaP positivo tá tudo ok. 
					 * Se deltaP negativo, então verifica-se se tem dispositivos armazenadores de energia. 
					 *  */
					if(deltaP<0){ 
						/**se entrar nesse if, será necessário consultar o XMl para averiguar se há dispositivos
						 * armazenadores de energia
						 */
//						List lista = agenteChaveBD.getChild("sentido").getChild("sentido2").getChild("outrasChaves").getChildren(); 
//
//						Iterator i = lista.iterator();
//
//						while(i.hasNext()) {
//							Element elemento = (Element) i.next();
//							String nome = String.valueOf(elemento.getText());
//							if (nome!= null && nome.length()>0 && nome!= "nenhum"){
//								
//								cont1 = cont1 + 1;
//								System.out.println("As outras chaves s�o: "+nome);
//								msg.addReceiver(new AID((String) nome, AID.ISLOCALNAME));
//
//							}
//						}// fim do while(i.hasNext())
						
//						msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
//						msg.setContent("abrir");
						
						/**
						 * Se tiver dispositivos armazenadores, manda-se um contracte net para todos ao mesmo tempo 
						 * solicitando um deltaP. Devo pegar um pouco de cada ou pego o de uma vez só? Como sei que só terá 
						 * um banco de baterias vou fazer pegando só de um dispositivo de armazenamento logo.
						 */
						/*if(){   //Se tem dispositivos armazenadores de energia, então
						*//*********************************************************************************
						 * FIPA Contract Net Initiator para negociação deltaP com sistemas de armazenamento de energia
						 *********************************************************************************//*
							ACLMessage negociar = new ACLMessage(ACLMessage.CFP);
							List lista1 = agenteAlimentadorBD.getChild("outrosALs").getChildren(); 
							Iterator i = lista1.iterator();
	
	
							while(i.hasNext()) {
	
								Element elemento = (Element) i.next();
	
								//String nome = String.valueOf(elemento.getText());
								String nome = String.valueOf(elemento.getText());
								System.out.println("-<<"+agenteAlimentador+">>: o outro AL � "+nome);
								//negociar.addReceiver(new AID((String) nome, AID.ISLOCALNAME));
								negociar.addReceiver(new AID( nome, AID.ISLOCALNAME));
	
							}
	
							// String carga = agenteAlimentadorBD.getChild("cargaPerdida").getAttributeValue("valor");
							negociar.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
							negociar.setContent(carga); //Carga perdida
	
							addBehaviour(new ContractNetInitiator(myAgent, negociar) {
	
								protected void handlePropose(ACLMessage propose, Vector v) {
									System.out.println("-<<"+agenteAlimentador+">>: o agente "+propose.getSender().getLocalName()+" disse "+propose.getContent());
								}
	
								protected void handleRefuse(ACLMessage refuse) {
									System.out.println("-<<"+agenteAlimentador+">>: o agente "+refuse.getSender().getLocalName()+" recusou");
								}
	
								protected void handleFailure(ACLMessage failure) {
									if (failure.getSender().equals(myAgent.getAMS())) {
	
										System.out.println("N�o existe outros agentes ALs");
									}
									else {
										System.out.println("-<<"+agenteAlimentador+">>: o agente "+failure.getSender().getLocalName()+" falhou");
									}
									// Immediate failure --> we will not receive a response from this agent
								}
	
								protected void handleAllResponses(Vector responses, Vector acceptances) {
	
								}// fim do handleAllResponses do comportamento Contract net
							}// fim do if para ver se 
*/					}// fim do if(deltaP<0) 
				}
			}
		});
		

					
					
				

	} // fim do public void setup

	/**
	 * Método para exibição de mensagens ACL
	 *  @param msg recebe uma mensagem to tipo ALCMessage
	 *  
	 */
	public void exibirMensagem(ACLMessage msg) {
		System.out.println("\n\n===============<<MENSAGEM>>==============");
		System.out.println("De: " + msg.getSender());
		System.out.println("Para: " + this.getName());
		System.out.println("Conteudo: " + msg.getContent());
		System.out.println("=============================================");
	}

	/**
	 *  Método para carregamento do XML
	 * 
	 */
	public Element carregaBD(String nomeAgente) {

		// Declaração de variáveis, uma de cada tipo
		File endereco = null; // Desnecessário se usarmos o endereço do arq.
								// dentro do doc dentro do try
		SAXBuilder builder = null; // usado p/ processar a estrut. do doc. p/
									// dentro da variável do tipo documento
		Document doc = null;
		Element BD = null;

		endereco = new File("src/XML/" + nomeAgente + ".xml");

		builder = new SAXBuilder();

		try { // Criando o arquivo propriamente dito
			doc = builder.build(endereco);
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}// fim do try

		BD = doc.getRootElement();
		return BD;

	}// fim do método carregarBD
}
